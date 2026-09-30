package com.medroute.service;

import com.medroute.dao.DemandDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.dao.MedicineDAO;
import com.medroute.model.DemandAnalysis;
import com.medroute.model.DemandMetrics;
import com.medroute.model.Facility;
import com.medroute.model.InventoryBatch;
import com.medroute.model.Medicine;
import com.medroute.model.RiskScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DemandService {

    private static final Logger logger = LoggerFactory.getLogger(DemandService.class);

    private final DemandDAO demandDAO;
    private final FacilityDAO facilityDAO;
    private final MedicineDAO medicineDAO;

    public DemandService(DemandDAO demandDAO, FacilityDAO facilityDAO, MedicineDAO medicineDAO) {
        this.demandDAO = demandDAO;
        this.facilityDAO = facilityDAO;
        this.medicineDAO = medicineDAO;
    }

    public String calculateFacilityRiskScore(Long facilityId) {
        // Find highest risk medicine in the facility. 
        // For Phase 5 we just use a generic random representation or aggregate. 
        // We will default to MODERATE if unknown.
        try {
            // Check count of active batches
            int batches = demandDAO.getActiveBatches(facilityId, null).size();
            if (batches == 0) return "UNKNOWN";
            // Return dummy aggregated logic based on batches size
            return (batches > 10) ? "LOW" : "MODERATE";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public DemandAnalysis analyzeMedicine(Long facilityId, Long medicineId) {
        Medicine medicine = medicineDAO.findById(medicineId)
            .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));
        
        Facility facility = facilityDAO.findById(facilityId)
            .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        List<InventoryBatch> batches = demandDAO.getActiveBatches(facilityId, medicineId);
        
        int currentStock = batches.stream().mapToInt(InventoryBatch::getQuantity).sum();
        int reservedStock = batches.stream().mapToInt(InventoryBatch::getReservedQuantity).sum();
        int availableStock = Math.max(0, currentStock - reservedStock);

        DemandMetrics metrics = new DemandMetrics();
        metrics.setFacilityId(facilityId);
        metrics.setMedicineId(medicineId);
        metrics.setMedicineName(medicine.getName());
        metrics.setCurrentStock(currentStock);
        metrics.setReservedStock(reservedStock);
        metrics.setAvailableStock(availableStock);

        boolean hasData = demandDAO.hasAnyConsumptionData(facilityId, medicineId);
        if (!hasData) {
            metrics.setDataStatus("NO_DATA");
            metrics.setAverageDailyConsumption(null);
            metrics.setDaysOfStock(null);
            metrics.setTrendPercent(null);
            metrics.setTrendClassification(null);
        } else {
            Double avgConsumption = calculateAverageDailyConsumption(facilityId, medicineId, 30);
            metrics.setAverageDailyConsumption(avgConsumption);
            
            Double daysOfStock = calculateDaysOfStock(availableStock, avgConsumption);
            metrics.setDaysOfStock(daysOfStock);

            Double trendPercent = calculateTrendPercent(facilityId, medicineId);
            metrics.setTrendPercent(trendPercent);

            if (trendPercent != null) {
                metrics.setTrendClassification(classifyTrend(trendPercent));
                metrics.setDataStatus("OK");
            } else {
                metrics.setTrendClassification(null);
                metrics.setDataStatus("INSUFFICIENT_BASELINE");
            }
        }

        // Expiry calculation
        Integer nearestExpiryDays = null;
        for (InventoryBatch b : batches) {
            if (b.getAvailableQuantity() > 0 && b.getExpiryDate() != null) {
                int days = (int) ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
                if (nearestExpiryDays == null || days < nearestExpiryDays) {
                    nearestExpiryDays = days;
                }
            }
        }
        metrics.setNearestExpiryDays(nearestExpiryDays);
        metrics.setExpiryRiskLevel(getExpiryRiskLevelString(nearestExpiryDays));

        // Risk Scoring
        RiskScore riskScore = new RiskScore();
        riskScore.setCoverageScore(calculateCoverageRisk(metrics.getDaysOfStock()));
        riskScore.setTrendScore(calculateTrendRisk(metrics.getTrendClassification()));
        riskScore.setEmergencyScore(calculateEmergencyRisk(facilityId, medicineId));
        riskScore.setExpiryScore(calculateExpiryRisk(nearestExpiryDays));
        riskScore.setPriorityScore(calculateFacilityPriorityRisk(facility));
        
        int finalScore = calculateFinalRiskScore(riskScore);
        riskScore.setFinalScore(finalScore);
        riskScore.setRiskLevel(getRiskLevelString(finalScore));

        DemandAnalysis analysis = new DemandAnalysis();
        analysis.setMetrics(metrics);
        analysis.setRiskScore(riskScore);
        analysis.setCalculatedAt(LocalDateTime.now());

        return analysis;
    }

    public Double calculateAverageDailyConsumption(Long facilityId, Long medicineId, int daysWindow) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(daysWindow);
        int totalConsumption = demandDAO.getConsumptionInPeriod(facilityId, medicineId, start, end);
        return (double) totalConsumption / daysWindow;
    }

    public Double calculateDaysOfStock(int availableStock, Double avgConsumption) {
        if (avgConsumption == null || avgConsumption <= 0) {
            return null;
        }
        return availableStock / avgConsumption;
    }

    public Double calculateTrendPercent(Long facilityId, Long medicineId) {
        LocalDate now = LocalDate.now();
        LocalDate recentStart = now.minusDays(7);
        LocalDate previousStart = recentStart.minusDays(7);

        int recentConsumption = demandDAO.getConsumptionInPeriod(facilityId, medicineId, recentStart, now);
        int previousConsumption = demandDAO.getConsumptionInPeriod(facilityId, medicineId, previousStart, recentStart);

        if (previousConsumption == 0) {
            return null;
        }
        return ((double) (recentConsumption - previousConsumption) / previousConsumption) * 100.0;
    }

    public String classifyTrend(Double trendPercent) {
        if (trendPercent == null) return null;
        if (trendPercent <= -20.0) return "STRONGLY_DECREASING";
        if (trendPercent <= -5.0) return "DECREASING";
        if (trendPercent <= 5.0) return "STABLE";
        if (trendPercent <= 20.0) return "INCREASING";
        return "STRONGLY_INCREASING";
    }

    private String getExpiryRiskLevelString(Integer nearestExpiryDays) {
        if (nearestExpiryDays == null) return "NORMAL";
        if (nearestExpiryDays < 0) return "EXPIRED";
        if (nearestExpiryDays <= 7) return "CRITICAL";
        if (nearestExpiryDays <= 30) return "WARNING";
        return "NORMAL";
    }

    public int calculateExpiryRisk(Integer nearestExpiryDays) {
        if (nearestExpiryDays == null) return 0; // No stock with expiry
        if (nearestExpiryDays < 0) return 100;
        if (nearestExpiryDays <= 7) return 90;
        if (nearestExpiryDays <= 30) return 60;
        if (nearestExpiryDays <= 60) return 30;
        return 10;
    }

    public int calculateCoverageRisk(Double daysOfStock) {
        if (daysOfStock == null) return 50; // Neutral fallback if no data
        if (daysOfStock <= 0) return 100;
        
        if (daysOfStock <= 1) {
            return interpolate(daysOfStock, 0, 1, 100, 95);
        } else if (daysOfStock <= 3) {
            return interpolate(daysOfStock, 1, 3, 95, 85);
        } else if (daysOfStock <= 7) {
            return interpolate(daysOfStock, 3, 7, 85, 70);
        } else if (daysOfStock <= 14) {
            return interpolate(daysOfStock, 7, 14, 70, 50);
        } else if (daysOfStock <= 30) {
            return interpolate(daysOfStock, 14, 30, 50, 25);
        } else {
            return 5;
        }
    }

    private int interpolate(Double val, double x0, double x1, double y0, double y1) {
        double ratio = (val - x0) / (x1 - x0);
        double interp = y0 + ratio * (y1 - y0);
        return (int) Math.round(interp);
    }

    public int calculateTrendRisk(String classification) {
        if (classification == null) return 40; // Default STABLE
        switch (classification) {
            case "STRONGLY_INCREASING": return 90;
            case "INCREASING": return 70;
            case "STABLE": return 40;
            case "DECREASING": return 20;
            case "STRONGLY_DECREASING": return 10;
            default: return 40;
        }
    }

    public int calculateEmergencyRisk(Long facilityId, Long medicineId) {
        int openRequests = demandDAO.countActiveEmergencyRequests(facilityId, medicineId);
        if (openRequests == 0) return 0;
        if (openRequests == 1) return 50;
        if (openRequests == 2) return 80;
        return 100;
    }

    public int calculateFacilityPriorityRisk(Facility facility) {
        // Fallback or neutral default as requested if no explicit field exists
        if (facility.getFacilityType() == com.medroute.model.FacilityType.HOSPITAL) {
            return 80;
        } else if (facility.getFacilityType() == com.medroute.model.FacilityType.CLINIC) {
            return 50;
        }
        return 30; // Pharmacy/NGO neutral
    }

    public int calculateFinalRiskScore(RiskScore r) {
        double weighted = r.getCoverageScore() * 0.40 +
                          r.getTrendScore() * 0.25 +
                          r.getEmergencyScore() * 0.20 +
                          r.getExpiryScore() * 0.10 +
                          r.getPriorityScore() * 0.05;
        
        int score = (int) Math.round(weighted);
        return Math.max(0, Math.min(100, score)); // Clamp [0, 100]
    }

    public String getRiskLevelString(int score) {
        if (score <= 30) return "LOW";
        if (score <= 60) return "MODERATE";
        if (score <= 80) return "HIGH";
        return "CRITICAL";
    }
}
