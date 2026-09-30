package com.medroute.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsDAOTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalyticsDAO analyticsDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInventoryTrends_queryIncludesFacilityFilter() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());
        
        analyticsDAO.getInventoryTrendsByDate(1L, start, end);
        
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        
        assertTrue(sqlCaptor.getValue().contains("AND facility_id = ?"));
    }

    @Test
    void testInventoryTrends_queryOmitsFacilityFilterWhenNull() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());
        
        analyticsDAO.getInventoryTrendsByDate(null, start, end);
        
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        
        assertTrue(!sqlCaptor.getValue().contains("AND facility_id = ?"));
    }

    @Test
    void testTransferVolume_dateRangeApplied() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());
        
        analyticsDAO.getTransferVolumeByDate(null, start, end);
        
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForList(anyString(), paramsCaptor.capture());
        
        Object[] params = paramsCaptor.getValue();
        assertEquals(start.atStartOfDay(), params[0]);
        assertEquals(end.plusDays(1).atStartOfDay(), params[1]);
    }

    @Test
    void testRiskDistribution_usesCorrectBucketLogic() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(Collections.singletonList(
                Map.of("critical_count", 5, "high_count", 10, "moderate_count", 15, "low_count", 20)
        ));
        
        Map<String, Object> result = analyticsDAO.getRiskDistribution(1L);
        
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("status = 'EXPIRED'"));
        assertTrue(sql.contains("quantity <= minimum_stock"));
        assertTrue(sql.contains("quantity > minimum_stock"));
        
        assertEquals(5, result.get("critical_count"));
    }

    @Test
    void testDemandPatterns_returnsEmptyListOnNoData() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(new ArrayList<>());
        
        List<Map<String, Object>> result = analyticsDAO.getDemandPatternsByMedicine(1L, start, end);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFacilityPerformance_aggregatesCorrectly() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(Collections.singletonList(
                Map.of("facility_name", "Test Fac", "total_transfers", 10)
        ));
        
        List<Map<String, Object>> result = analyticsDAO.getFacilityPerformanceMetrics(start, end);
        
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        
        assertTrue(sqlCaptor.getValue().contains("GROUP BY f.id, f.name"));
        assertEquals("Test Fac", result.get(0).get("facility_name"));
    }
}
