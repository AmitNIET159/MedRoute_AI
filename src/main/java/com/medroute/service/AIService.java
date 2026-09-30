package com.medroute.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medroute.dao.AIDAO;
import com.medroute.model.AIInsight;
import com.medroute.model.AIUsageLog;
import com.medroute.model.DemandAnalysis;
import com.medroute.model.InsightType;
import com.medroute.model.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final AIDAO aiDAO;
    private final HuggingFaceClient hfClient;
    private final ObjectMapper objectMapper;
    private final int cacheTtlHours;

    public AIService(AIDAO aiDAO, HuggingFaceClient hfClient, ObjectMapper objectMapper,
                     io.github.cdimascio.dotenv.Dotenv dotenv) {
        this.aiDAO = aiDAO;
        this.hfClient = hfClient;
        this.objectMapper = objectMapper;
        
        String ttl = dotenv.get("AI_CACHE_TTL_HOURS");
        this.cacheTtlHours = (ttl != null && !ttl.trim().isEmpty()) ? Integer.parseInt(ttl) : 6;
    }

    public AIInsight getInsight(Long facilityId, Long medicineId, DemandAnalysis analysis, Long userId) {
        String context = buildPromptContext(analysis);
        String requestHash = generateRequestHash(context);

        // Check Cache
        Optional<AIInsight> cached = aiDAO.findCachedInsight(requestHash);
        if (cached.isPresent()) {
            logger.info("AI Cache hit for facility {}, medicine {}", facilityId, medicineId);
            logUsage(userId, "SHORTAGE_RISK", hfClient.getModelName(), 0, 0, 0, true, "SUCCESS");
            return cached.get();
        }

        // Call API
        logger.info("AI Cache miss for facility {}, medicine {}, calling HF API", facilityId, medicineId);
        String prompt = "You are a healthcare logistics analysis assistant. " +
                "You do not provide medical advice. You do not diagnose. You do not prescribe. " +
                "You do not provide dosage. You do not make clinical decisions. " +
                "Interpret ONLY the supplied logistics data. Do not invent missing values. " +
                "Do not modify the deterministic risk score. Provide a concise logistics explanation.\n\n" +
                context;

        HuggingFaceClient.HFResponse hfResponse = hfClient.generateInsight(prompt);

        if ("SUCCESS".equals(hfResponse.status)) {
            // Validate response minimally
            if (hfResponse.text != null && hfResponse.text.length() > 20) {
                logUsage(userId, "SHORTAGE_RISK", hfClient.getModelName(), hfResponse.inputTokens, hfResponse.outputTokens, hfResponse.durationMs, false, "SUCCESS");
                
                AIInsight insight = buildInsight(facilityId, medicineId, analysis, hfResponse.text, requestHash);
                try {
                    Long id = aiDAO.createInsight(insight);
                    insight.setId(id);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    logger.info("Duplicate request_hash on SUCCESS insert, returning existing");
                    Optional<AIInsight> existing = aiDAO.findByRequestHash(requestHash);
                    if (existing.isPresent()) return existing.get();
                }
                return insight;
            }
        }

        // Fallback
        logUsage(userId, "SHORTAGE_RISK", hfClient.getModelName(), hfResponse.inputTokens, hfResponse.outputTokens, hfResponse.durationMs, false, hfResponse.status);
        logger.warn("Using deterministic fallback for facility {}, medicine {} due to HF status {}", facilityId, medicineId, hfResponse.status);
        
        AIInsight fallback = generateFallbackInsight(facilityId, medicineId, analysis);
        fallback.setRequestHash(requestHash);
        try {
            Long fallbackId = aiDAO.createInsight(fallback);
            fallback.setId(fallbackId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Expired cache row still exists with same request_hash — return it or the fallback as-is
            logger.info("Duplicate request_hash detected, returning existing or in-memory fallback");
            Optional<AIInsight> existing = aiDAO.findByRequestHash(requestHash);
            if (existing.isPresent()) {
                return existing.get();
            }
            // If lookup also fails, return the in-memory fallback without persisting
        }
        return fallback;
    }

    private AIInsight buildInsight(Long facilityId, Long medicineId, DemandAnalysis analysis, String text, String hash) {
        AIInsight insight = new AIInsight();
        insight.setFacilityId(facilityId);
        insight.setMedicineId(medicineId);
        insight.setInsightType(InsightType.SHORTAGE_RISK);
        
        String riskLevel = analysis.getRiskScore().getRiskLevel();
        insight.setTitle("Logistics Analysis: " + riskLevel + " Risk");
        
        // Append disclaimer to content
        insight.setContent(text + "\n\n" + AIInsight.AI_DISCLAIMER);
        
        insight.setSeverity(mapRiskToSeverity(riskLevel));
        insight.setRiskScore(BigDecimal.valueOf(analysis.getRiskScore().getFinalScore()));
        
        try {
            insight.setMetadata(objectMapper.writeValueAsString(analysis.getMetrics()));
        } catch (JsonProcessingException e) {
            insight.setMetadata("{}");
        }
        
        insight.setRequestHash(hash);
        insight.setExpiresAt(LocalDateTime.now().plusHours(cacheTtlHours));
        return insight;
    }

    private AIInsight generateFallbackInsight(Long facilityId, Long medicineId, DemandAnalysis analysis) {
        String riskLevel = analysis.getRiskScore().getRiskLevel();
        String text;
        switch (riskLevel) {
            case "CRITICAL":
                text = "Inventory is at critical logistics risk based on current stock coverage and consumption trends. Review available supply at nearby facilities.";
                break;
            case "HIGH":
                text = "Inventory risk is elevated. Current stock levels may not sustain projected demand. Consider scheduling replenishment.";
                break;
            case "MODERATE":
                text = "Stock levels are within acceptable range but require monitoring. Consumption patterns suggest periodic review.";
                break;
            case "LOW":
            default:
                text = "Inventory levels are healthy. Current stock coverage exceeds projected demand.";
                break;
        }

        AIInsight insight = buildInsight(facilityId, medicineId, analysis, text, null);
        // Replace disclaimer for fallback
        insight.setContent(text + "\n\nDeterministic logistics analysis. This system does not provide medical advice.");
        return insight;
    }

    private Severity mapRiskToSeverity(String riskLevel) {
        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) return Severity.CRITICAL;
        if ("MODERATE".equals(riskLevel)) return Severity.WARNING;
        return Severity.INFO;
    }

    private String buildPromptContext(DemandAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Medicine: ").append(analysis.getMetrics().getMedicineName()).append("\n");
        sb.append("Available stock: ").append(analysis.getMetrics().getAvailableStock()).append("\n");
        if (analysis.getMetrics().getAverageDailyConsumption() != null) {
            sb.append("Average daily consumption: ").append(String.format("%.2f", analysis.getMetrics().getAverageDailyConsumption())).append("\n");
        } else {
            sb.append("Average daily consumption: Insufficient Data\n");
        }
        if (analysis.getMetrics().getDaysOfStock() != null) {
            sb.append("Days of stock: ").append(String.format("%.1f", analysis.getMetrics().getDaysOfStock())).append("\n");
        } else {
            sb.append("Days of stock: Insufficient Data\n");
        }
        if (analysis.getMetrics().getTrendPercent() != null) {
            sb.append("Consumption trend: ").append(String.format("%+.1f%%", analysis.getMetrics().getTrendPercent())).append("\n");
            sb.append("Trend: ").append(analysis.getMetrics().getTrendClassification()).append("\n");
        } else {
            sb.append("Consumption trend: Insufficient Data\n");
        }
        sb.append("Expiry risk: ").append(analysis.getMetrics().getExpiryRiskLevel()).append("\n");
        sb.append("Risk score: ").append(analysis.getRiskScore().getFinalScore()).append("\n");
        sb.append("Risk level: ").append(analysis.getRiskScore().getRiskLevel()).append("\n");
        return sb.toString();
    }

    private String generateRequestHash(String context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(context.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(context.hashCode()); // Fallback
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void logUsage(Long userId, String requestType, String model, int inTokens, int outTokens, int timeMs, boolean cached, String status) {
        AIUsageLog log = new AIUsageLog();
        log.setUserId(userId);
        log.setRequestType(requestType);
        log.setModelUsed(model);
        log.setInputTokens(inTokens);
        log.setOutputTokens(outTokens);
        log.setResponseTimeMs(timeMs);
        log.setIsCached(cached);
        log.setStatus(status);
        try {
            aiDAO.logUsage(log);
        } catch (Exception e) {
            logger.error("Failed to write AI usage log", e);
        }
    }
}
