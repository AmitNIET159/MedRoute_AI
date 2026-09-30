package com.medroute.service;

import com.medroute.dao.AnalyticsDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    @Mock
    private AnalyticsDAO analyticsDAO;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInventoryTrends_withData() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        
        List<Map<String, Object>> mockData = new ArrayList<>();
        mockData.add(Map.of("trend_date", "2023-10-01", "transaction_type", "RECEIVED", "total_quantity", 100));
        mockData.add(Map.of("trend_date", "2023-10-01", "transaction_type", "DISPENSED", "total_quantity", 50));
        mockData.add(Map.of("trend_date", "2023-10-02", "transaction_type", "RECEIVED", "total_quantity", 200));

        when(analyticsDAO.getInventoryTrendsByDate(1L, start, end)).thenReturn(mockData);

        Map<String, Object> result = analyticsService.getInventoryTrends(1L, start, end);

        assertNotNull(result.get("labels"));
        assertNotNull(result.get("datasets"));
        
        List<String> labels = (List<String>) result.get("labels");
        assertEquals(2, labels.size());
        assertEquals("2023-10-01", labels.get(0));
        
        Map<String, List<Number>> datasets = (Map<String, List<Number>>) result.get("datasets");
        assertEquals(100, datasets.get("RECEIVED").get(0));
        assertEquals(50, datasets.get("DISPENSED").get(0));
        assertEquals(0, datasets.get("CONSUMPTION").get(0));
    }

    @Test
    void testInventoryTrends_emptyData() {
        when(analyticsDAO.getInventoryTrendsByDate(any(), any(), any())).thenReturn(new ArrayList<>());
        
        Map<String, Object> result = analyticsService.getInventoryTrends(1L, null, null);
        
        assertEquals(0, ((List<?>) result.get("labels")).size());
        assertNotNull(((Map<?, ?>) result.get("datasets")).get("RECEIVED"));
    }

    @Test
    void testInventoryTrends_defaultDateRange() {
        when(analyticsDAO.getInventoryTrendsByDate(any(), any(), any())).thenReturn(new ArrayList<>());
        analyticsService.getInventoryTrends(1L, null, null);
        verify(analyticsDAO).getInventoryTrendsByDate(eq(1L), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testTransferMetrics_withData() {
        List<Map<String, Object>> mockData = new ArrayList<>();
        mockData.add(Map.of("trend_date", "2023-10-01", "status", "COMPLETED", "transfer_count", 5));
        when(analyticsDAO.getTransferVolumeByDate(any(), any(), any())).thenReturn(mockData);

        Map<String, Object> result = analyticsService.getTransferMetrics(1L, null, null);
        
        List<String> labels = (List<String>) result.get("labels");
        assertEquals(1, labels.size());
        
        Map<String, List<Number>> datasets = (Map<String, List<Number>>) result.get("datasets");
        assertEquals(5, datasets.get("COMPLETED").get(0));
        assertEquals(0, datasets.get("REQUESTED").get(0));
    }

    @Test
    void testTransferMetrics_emptyData() {
        when(analyticsDAO.getTransferVolumeByDate(any(), any(), any())).thenReturn(new ArrayList<>());
        Map<String, Object> result = analyticsService.getTransferMetrics(1L, null, null);
        assertEquals(0, ((List<?>) result.get("labels")).size());
    }

    @Test
    void testFacilityPerformance_withData() {
        List<Map<String, Object>> mockData = new ArrayList<>();
        mockData.add(Map.of("facility_name", "Fac A", "total_transfers", 10L, "completed_transfers", 8L, "avg_quantity", 100.5, "avg_distance", 12.0));
        when(analyticsDAO.getFacilityPerformanceMetrics(any(), any())).thenReturn(mockData);

        Map<String, Object> result = analyticsService.getFacilityPerformance(null, null);
        
        List<String> labels = (List<String>) result.get("labels");
        assertEquals("Fac A", labels.get(0));
        assertEquals(10L, ((List<Number>) result.get("totalTransfers")).get(0));
        assertEquals(100.5, ((List<Number>) result.get("avgQuantity")).get(0));
    }

    @Test
    void testFacilityPerformance_emptyData() {
        when(analyticsDAO.getFacilityPerformanceMetrics(any(), any())).thenReturn(new ArrayList<>());
        Map<String, Object> result = analyticsService.getFacilityPerformance(null, null);
        assertEquals(0, ((List<?>) result.get("labels")).size());
    }

    @Test
    void testDemandPatterns_withData() {
        List<Map<String, Object>> byUrgency = Collections.singletonList(Map.of("urgency", "CRITICAL", "request_count", 15L));
        List<Map<String, Object>> byMed = Collections.singletonList(Map.of("medicine_name", "Paracetamol", "total_needed", 500L));
        
        when(analyticsDAO.getDemandPatternsByUrgency(any(), any(), any())).thenReturn(byUrgency);
        when(analyticsDAO.getDemandPatternsByMedicine(any(), any(), any())).thenReturn(byMed);

        Map<String, Object> result = analyticsService.getDemandPatterns(1L, null, null);
        
        Map<String, Number> urgency = (Map<String, Number>) result.get("urgency");
        assertEquals(15L, urgency.get("CRITICAL"));
        
        List<String> medLabels = (List<String>) result.get("medicineLabels");
        assertEquals("Paracetamol", medLabels.get(0));
    }

    @Test
    void testDemandPatterns_emptyData() {
        when(analyticsDAO.getDemandPatternsByUrgency(any(), any(), any())).thenReturn(new ArrayList<>());
        when(analyticsDAO.getDemandPatternsByMedicine(any(), any(), any())).thenReturn(new ArrayList<>());
        Map<String, Object> result = analyticsService.getDemandPatterns(1L, null, null);
        assertEquals(0, ((Map<?, ?>) result.get("urgency")).size());
    }

    @Test
    void testRiskDistribution_withData() {
        Map<String, Object> mockData = Map.of("critical_count", 5, "high_count", 10, "moderate_count", 15, "low_count", 20);
        when(analyticsDAO.getRiskDistribution(1L)).thenReturn(mockData);

        Map<String, Object> result = analyticsService.getRiskDistribution(1L);
        assertEquals(5, result.get("CRITICAL"));
        assertEquals(20, result.get("LOW"));
    }

    @Test
    void testRiskDistribution_emptyData() {
        when(analyticsDAO.getRiskDistribution(1L)).thenReturn(null);
        Map<String, Object> result = analyticsService.getRiskDistribution(1L);
        assertEquals(0, result.get("CRITICAL"));
    }

    @Test
    void testFacilityIsolation() {
        analyticsService.getRiskDistribution(99L);
        verify(analyticsDAO).getRiskDistribution(99L);
        
        analyticsService.getRiskDistribution(null);
        verify(analyticsDAO).getRiskDistribution(null);
    }
}
