package com.medroute.service;

import com.medroute.dao.DemandDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.dao.MedicineDAO;
import com.medroute.model.DemandAnalysis;
import com.medroute.model.Facility;
import com.medroute.model.FacilityType;
import com.medroute.model.InventoryBatch;
import com.medroute.model.Medicine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DemandServiceTest {

    private DemandDAO demandDAO;
    private FacilityDAO facilityDAO;
    private MedicineDAO medicineDAO;
    private DemandService demandService;

    @BeforeEach
    void setUp() {
        demandDAO = Mockito.mock(DemandDAO.class);
        facilityDAO = Mockito.mock(FacilityDAO.class);
        medicineDAO = Mockito.mock(MedicineDAO.class);
        demandService = new DemandService(demandDAO, facilityDAO, medicineDAO);
    }

    @Test
    void testCalculateAverageDailyConsumption_Normal() {
        when(demandDAO.getConsumptionInPeriod(eq(1L), eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(300);
        Double avg = demandService.calculateAverageDailyConsumption(1L, 1L, 30);
        assertEquals(10.0, avg, 0.001);
    }

    @Test
    void testCalculateAverageDailyConsumption_NoData() {
        when(demandDAO.getConsumptionInPeriod(eq(1L), eq(1L), any(), any())).thenReturn(0);
        Double avg = demandService.calculateAverageDailyConsumption(1L, 1L, 30);
        assertEquals(0.0, avg, 0.001);
    }

    @Test
    void testCalculateDaysOfStock_Normal() {
        assertEquals(3.5, demandService.calculateDaysOfStock(35, 10.0), 0.001);
    }

    @Test
    void testCalculateDaysOfStock_ZeroConsumption() {
        assertNull(demandService.calculateDaysOfStock(10, 0.0));
    }

    @Test
    void testCalculateTrendPercent_Normal() {
        // Mock getConsumptionInPeriod specifically is tricky due to date constraints in mockito,
        // so testing calculateTrendPercent logic based on mock setup
        when(demandDAO.getConsumptionInPeriod(any(), any(), any(), any()))
                .thenReturn(100) // first call recent (but wait, args matter)
                .thenReturn(50); // previous

        // Since it relies on order and precise dates, we will mock them generically
        when(demandDAO.getConsumptionInPeriod(eq(1L), eq(1L), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    LocalDate to = invocation.getArgument(3);
                    if (from.isEqual(LocalDate.now().minusDays(7))) return 118; // recent
                    return 100; // previous
                });

        Double trend = demandService.calculateTrendPercent(1L, 1L);
        assertEquals(18.0, trend, 0.001);
    }

    @Test
    void testClassifyTrend_Boundaries() {
        assertEquals("STRONGLY_DECREASING", demandService.classifyTrend(-20.0));
        assertEquals("DECREASING", demandService.classifyTrend(-19.9));
        assertEquals("DECREASING", demandService.classifyTrend(-5.0));
        assertEquals("STABLE", demandService.classifyTrend(-4.9));
        assertEquals("STABLE", demandService.classifyTrend(5.0));
        assertEquals("INCREASING", demandService.classifyTrend(5.1));
        assertEquals("INCREASING", demandService.classifyTrend(20.0));
        assertEquals("STRONGLY_INCREASING", demandService.classifyTrend(20.1));
    }

    @Test
    void testCoverageRiskBreakpoints() {
        assertEquals(100, demandService.calculateCoverageRisk(0.0));
        assertEquals(95, demandService.calculateCoverageRisk(1.0));
        assertEquals(85, demandService.calculateCoverageRisk(3.0));
        assertEquals(70, demandService.calculateCoverageRisk(7.0));
        assertEquals(50, demandService.calculateCoverageRisk(14.0));
        assertEquals(25, demandService.calculateCoverageRisk(30.0));
        assertEquals(5, demandService.calculateCoverageRisk(31.0));
        
        // Interpolation test
        assertEquals(90, demandService.calculateCoverageRisk(2.0));
    }

    @Test
    void testExpiryRisk() {
        assertEquals(100, demandService.calculateExpiryRisk(-1)); // expired
        assertEquals(90, demandService.calculateExpiryRisk(0)); // expired today
        assertEquals(90, demandService.calculateExpiryRisk(7));
        assertEquals(60, demandService.calculateExpiryRisk(30));
        assertEquals(30, demandService.calculateExpiryRisk(60));
        assertEquals(10, demandService.calculateExpiryRisk(61));
        assertEquals(0, demandService.calculateExpiryRisk(null)); // no data
    }

    @Test
    void testRiskLevel() {
        assertEquals("LOW", demandService.getRiskLevelString(0));
        assertEquals("LOW", demandService.getRiskLevelString(30));
        assertEquals("MODERATE", demandService.getRiskLevelString(31));
        assertEquals("MODERATE", demandService.getRiskLevelString(60));
        assertEquals("HIGH", demandService.getRiskLevelString(61));
        assertEquals("HIGH", demandService.getRiskLevelString(80));
        assertEquals("CRITICAL", demandService.getRiskLevelString(81));
        assertEquals("CRITICAL", demandService.getRiskLevelString(100));
    }

    @Test
    void testFullAnalysis_NoData() {
        Medicine m = new Medicine(); m.setId(1L); m.setName("Aspirin");
        Facility f = new Facility(); f.setId(1L); f.setFacilityType(FacilityType.CLINIC);
        
        when(medicineDAO.findById(1L)).thenReturn(Optional.of(m));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(f));
        when(demandDAO.hasAnyConsumptionData(1L, 1L)).thenReturn(false);
        when(demandDAO.getActiveBatches(1L, 1L)).thenReturn(Collections.emptyList());
        when(demandDAO.countActiveEmergencyRequests(1L, 1L)).thenReturn(0);

        DemandAnalysis analysis = demandService.analyzeMedicine(1L, 1L);
        
        assertEquals("NO_DATA", analysis.getMetrics().getDataStatus());
        assertNull(analysis.getMetrics().getAverageDailyConsumption());
        assertNull(analysis.getMetrics().getDaysOfStock());
        
        // Neutral risk if no data
        assertNotNull(analysis.getRiskScore());
        // coverage=50, trend=40, emerg=0, exp=0, prio=50
        // (50*0.4)+(40*0.25)+(0)+(0)+(50*0.05) = 20 + 10 + 2.5 = 32.5 = 33
        assertEquals(33, analysis.getRiskScore().getFinalScore());
        assertEquals("MODERATE", analysis.getRiskScore().getRiskLevel());
    }
}
