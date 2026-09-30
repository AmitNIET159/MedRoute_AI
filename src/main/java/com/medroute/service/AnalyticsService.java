package com.medroute.service;

import com.medroute.dao.AnalyticsDAO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AnalyticsDAO analyticsDAO;

    public AnalyticsService(AnalyticsDAO analyticsDAO) {
        this.analyticsDAO = analyticsDAO;
    }

    private LocalDate getOrDefaultStartDate(LocalDate startDate, int daysAgo) {
        return startDate != null ? startDate : LocalDate.now().minusDays(daysAgo);
    }

    private LocalDate getOrDefaultEndDate(LocalDate endDate) {
        return endDate != null ? endDate : LocalDate.now();
    }

    public Map<String, Object> getInventoryTrends(Long facilityId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = getOrDefaultStartDate(startDate, 30);
        LocalDate end = getOrDefaultEndDate(endDate);

        List<Map<String, Object>> trends = analyticsDAO.getInventoryTrendsByDate(facilityId, start, end);
        
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        Map<String, List<Number>> datasets = new HashMap<>();
        
        // Initialize datasets
        String[] types = {"RECEIVED", "DISPENSED", "TRANSFERRED_IN", "TRANSFERRED_OUT", "CONSUMPTION"};
        for (String type : types) {
            datasets.put(type, new ArrayList<>());
        }

        // We could fill missing dates for a continuous timeline, but for simplicity we rely on data presence.
        // Group by date
        Map<String, Map<String, Number>> dateData = new HashMap<>();
        for (Map<String, Object> row : trends) {
            String date = row.get("trend_date").toString();
            String type = (String) row.get("transaction_type");
            Number quantity = (Number) row.get("total_quantity");
            
            dateData.putIfAbsent(date, new HashMap<>());
            dateData.get(date).put(type, quantity);
            
            if (!labels.contains(date)) {
                labels.add(date);
            }
        }

        // Fill datasets
        for (String date : labels) {
            Map<String, Number> typeMap = dateData.get(date);
            for (String type : types) {
                datasets.get(type).add(typeMap.getOrDefault(type, 0));
            }
        }

        result.put("labels", labels);
        result.put("datasets", datasets);
        return result;
    }

    public Map<String, Object> getTransferMetrics(Long facilityId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = getOrDefaultStartDate(startDate, 30);
        LocalDate end = getOrDefaultEndDate(endDate);

        List<Map<String, Object>> volume = analyticsDAO.getTransferVolumeByDate(facilityId, start, end);
        
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        Map<String, List<Number>> datasets = new HashMap<>();
        
        String[] statuses = {"REQUESTED", "ACCEPTED", "IN_TRANSIT", "COMPLETED", "CANCELLED"};
        for (String status : statuses) {
            datasets.put(status, new ArrayList<>());
        }

        Map<String, Map<String, Number>> dateData = new HashMap<>();
        for (Map<String, Object> row : volume) {
            String date = row.get("trend_date").toString();
            String status = (String) row.get("status");
            Number count = (Number) row.get("transfer_count");
            
            dateData.putIfAbsent(date, new HashMap<>());
            dateData.get(date).put(status, count);
            
            if (!labels.contains(date)) {
                labels.add(date);
            }
        }

        for (String date : labels) {
            Map<String, Number> statusMap = dateData.get(date);
            for (String status : statuses) {
                datasets.get(status).add(statusMap.getOrDefault(status, 0));
            }
        }

        result.put("labels", labels);
        result.put("datasets", datasets);
        return result;
    }

    public Map<String, Object> getFacilityPerformance(LocalDate startDate, LocalDate endDate) {
        LocalDate start = getOrDefaultStartDate(startDate, 90);
        LocalDate end = getOrDefaultEndDate(endDate);

        List<Map<String, Object>> performance = analyticsDAO.getFacilityPerformanceMetrics(start, end);
        
        Map<String, Object> result = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Number> totalTransfers = new ArrayList<>();
        List<Number> completedTransfers = new ArrayList<>();
        List<Number> avgQuantity = new ArrayList<>();
        List<Number> avgDistance = new ArrayList<>();
        
        for (Map<String, Object> row : performance) {
            labels.add((String) row.get("facility_name"));
            totalTransfers.add((Number) row.get("total_transfers"));
            completedTransfers.add((Number) row.get("completed_transfers"));
            
            Number qty = (Number) row.get("avg_quantity");
            avgQuantity.add(qty != null ? qty : 0);
            
            Number dist = (Number) row.get("avg_distance");
            avgDistance.add(dist != null ? dist : 0);
        }

        result.put("labels", labels);
        result.put("totalTransfers", totalTransfers);
        result.put("completedTransfers", completedTransfers);
        result.put("avgQuantity", avgQuantity);
        result.put("avgDistance", avgDistance);
        
        return result;
    }

    public Map<String, Object> getDemandPatterns(Long facilityId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = getOrDefaultStartDate(startDate, 30);
        LocalDate end = getOrDefaultEndDate(endDate);

        List<Map<String, Object>> byUrgency = analyticsDAO.getDemandPatternsByUrgency(facilityId, start, end);
        List<Map<String, Object>> byMedicine = analyticsDAO.getDemandPatternsByMedicine(facilityId, start, end);
        
        Map<String, Object> result = new HashMap<>();
        
        // Urgency data
        Map<String, Number> urgencyMap = new HashMap<>();
        for (Map<String, Object> row : byUrgency) {
            urgencyMap.put((String) row.get("urgency"), (Number) row.get("request_count"));
        }
        result.put("urgency", urgencyMap);
        
        // Medicine data
        List<String> medicineLabels = new ArrayList<>();
        List<Number> medicineQuantities = new ArrayList<>();
        for (Map<String, Object> row : byMedicine) {
            medicineLabels.add((String) row.get("medicine_name"));
            medicineQuantities.add((Number) row.get("total_needed"));
        }
        result.put("medicineLabels", medicineLabels);
        result.put("medicineQuantities", medicineQuantities);
        
        return result;
    }

    public Map<String, Object> getRiskDistribution(Long facilityId) {
        Map<String, Object> riskData = analyticsDAO.getRiskDistribution(facilityId);
        
        Map<String, Object> result = new HashMap<>();
        if (riskData == null || riskData.isEmpty()) {
            result.put("CRITICAL", 0);
            result.put("HIGH", 0);
            result.put("MODERATE", 0);
            result.put("LOW", 0);
            return result;
        }

        result.put("CRITICAL", getNumberOrZero(riskData.get("critical_count")));
        result.put("HIGH", getNumberOrZero(riskData.get("high_count")));
        result.put("MODERATE", getNumberOrZero(riskData.get("moderate_count")));
        result.put("LOW", getNumberOrZero(riskData.get("low_count")));
        
        return result;
    }

    private Number getNumberOrZero(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        return 0;
    }
}
