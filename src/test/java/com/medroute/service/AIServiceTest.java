package com.medroute.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medroute.dao.AIDAO;
import com.medroute.model.AIInsight;
import com.medroute.model.DemandAnalysis;
import com.medroute.model.DemandMetrics;
import com.medroute.model.RiskScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AIServiceTest {

    private AIDAO aiDAO;
    private HuggingFaceClient hfClient;
    private ObjectMapper objectMapper;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        aiDAO = Mockito.mock(AIDAO.class);
        hfClient = Mockito.mock(HuggingFaceClient.class);
        objectMapper = new ObjectMapper();
        io.github.cdimascio.dotenv.Dotenv mockDotenv = Mockito.mock(io.github.cdimascio.dotenv.Dotenv.class);
        when(mockDotenv.get("AI_CACHE_TTL_HOURS")).thenReturn("6");
        aiService = new AIService(aiDAO, hfClient, objectMapper, mockDotenv);
    }

    private DemandAnalysis createMockAnalysis(String riskLevel) {
        DemandMetrics metrics = new DemandMetrics();
        metrics.setMedicineName("Test Med");
        metrics.setAvailableStock(100);
        metrics.setAverageDailyConsumption(10.0);
        metrics.setDaysOfStock(10.0);
        metrics.setTrendPercent(5.0);
        metrics.setTrendClassification("STABLE");
        metrics.setExpiryRiskLevel("NORMAL");

        RiskScore riskScore = new RiskScore();
        riskScore.setFinalScore(50);
        riskScore.setRiskLevel(riskLevel);

        DemandAnalysis analysis = new DemandAnalysis();
        analysis.setMetrics(metrics);
        analysis.setRiskScore(riskScore);
        return analysis;
    }

    @Test
    void testGetInsight_CacheHit() {
        DemandAnalysis analysis = createMockAnalysis("MODERATE");
        AIInsight cachedInsight = new AIInsight();
        cachedInsight.setContent("Cached content");
        
        when(aiDAO.findCachedInsight(anyString())).thenReturn(Optional.of(cachedInsight));

        AIInsight result = aiService.getInsight(1L, 1L, analysis, 1L);

        assertEquals("Cached content", result.getContent());
        verify(hfClient, never()).generateInsight(anyString());
        verify(aiDAO, times(1)).logUsage(any());
    }

    @Test
    void testGetInsight_CacheMiss_HFSuccess() {
        DemandAnalysis analysis = createMockAnalysis("MODERATE");
        when(aiDAO.findCachedInsight(anyString())).thenReturn(Optional.empty());
        
        HuggingFaceClient.HFResponse hfResponse = new HuggingFaceClient.HFResponse(
                "HF Explanation generated here.", "SUCCESS", null, 100, 50, 20);
        when(hfClient.generateInsight(anyString())).thenReturn(hfResponse);
        when(aiDAO.createInsight(any())).thenReturn(1L);

        AIInsight result = aiService.getInsight(1L, 1L, analysis, 1L);

        assertTrue(result.getContent().contains("HF Explanation generated here."));
        assertTrue(result.getContent().contains(AIInsight.AI_DISCLAIMER));
        verify(hfClient, times(1)).generateInsight(anyString());
        verify(aiDAO, times(1)).createInsight(any());
        verify(aiDAO, times(1)).logUsage(any());
    }

    @Test
    void testGetInsight_CacheMiss_HFFailure_Fallback() {
        DemandAnalysis analysis = createMockAnalysis("CRITICAL");
        when(aiDAO.findCachedInsight(anyString())).thenReturn(Optional.empty());
        
        HuggingFaceClient.HFResponse hfResponse = new HuggingFaceClient.HFResponse(
                null, "TIMEOUT", "Request timed out", 15000, 50, 0);
        when(hfClient.generateInsight(anyString())).thenReturn(hfResponse);
        when(aiDAO.createInsight(any())).thenReturn(1L);

        AIInsight result = aiService.getInsight(1L, 1L, analysis, 1L);

        assertTrue(result.getContent().contains("Inventory is at critical logistics risk"));
        assertTrue(result.getContent().contains("Deterministic logistics analysis."));
        verify(hfClient, times(1)).generateInsight(anyString());
        verify(aiDAO, times(1)).createInsight(any());
    }
}
