package com.medroute.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsDAO {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getInventoryTrendsByDate(Long facilityId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT DATE(created_at) as trend_date, transaction_type, SUM(quantity) as total_quantity " +
            "FROM inventory_transactions WHERE created_at >= ? AND created_at < ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(startDate.atStartOfDay());
        params.add(endDate.plusDays(1).atStartOfDay());

        if (facilityId != null) {
            sql.append("AND facility_id = ? ");
            params.add(facilityId);
        }

        sql.append("GROUP BY DATE(created_at), transaction_type ORDER BY trend_date ASC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getTransferVolumeByDate(Long facilityId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT DATE(created_at) as trend_date, status, COUNT(*) as transfer_count, SUM(requested_quantity) as total_quantity " +
            "FROM transfer_requests WHERE created_at >= ? AND created_at < ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(startDate.atStartOfDay());
        params.add(endDate.plusDays(1).atStartOfDay());

        if (facilityId != null) {
            sql.append("AND (from_facility_id = ? OR to_facility_id = ?) ");
            params.add(facilityId);
            params.add(facilityId);
        }

        sql.append("GROUP BY DATE(created_at), status ORDER BY trend_date ASC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getFacilityPerformanceMetrics(LocalDate startDate, LocalDate endDate) {
        String sql = 
            "SELECT f.name as facility_name, " +
            "COUNT(tr.id) as total_transfers, " +
            "SUM(CASE WHEN tr.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_transfers, " +
            "AVG(tr.requested_quantity) as avg_quantity, " +
            "AVG(tr.distance_km) as avg_distance " +
            "FROM facilities f " +
            "JOIN transfer_requests tr ON f.id = tr.from_facility_id OR f.id = tr.to_facility_id " +
            "WHERE tr.created_at >= ? AND tr.created_at < ? " +
            "GROUP BY f.id, f.name " +
            "ORDER BY total_transfers DESC " +
            "LIMIT 10";
            
        return jdbcTemplate.queryForList(sql, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    public List<Map<String, Object>> getDemandPatternsByUrgency(Long facilityId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT urgency, COUNT(*) as request_count " +
            "FROM emergency_requests WHERE created_at >= ? AND created_at < ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(startDate.atStartOfDay());
        params.add(endDate.plusDays(1).atStartOfDay());

        if (facilityId != null) {
            sql.append("AND requesting_facility_id = ? ");
            params.add(facilityId);
        }

        sql.append("GROUP BY urgency ORDER BY request_count DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getDemandPatternsByMedicine(Long facilityId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT m.name as medicine_name, SUM(er.quantity_needed) as total_needed " +
            "FROM emergency_requests er " +
            "JOIN medicine_catalog m ON er.medicine_id = m.id " +
            "WHERE er.created_at >= ? AND er.created_at < ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(startDate.atStartOfDay());
        params.add(endDate.plusDays(1).atStartOfDay());

        if (facilityId != null) {
            sql.append("AND er.requesting_facility_id = ? ");
            params.add(facilityId);
        }

        sql.append("GROUP BY m.id, m.name ORDER BY total_needed DESC LIMIT 10");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getRiskDistribution(Long facilityId) {
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "SUM(CASE WHEN status = 'EXPIRED' OR expiry_date < CURRENT_DATE THEN 1 ELSE 0 END) as critical_count, " +
            "SUM(CASE WHEN quantity <= minimum_stock AND status != 'EXPIRED' AND expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END) as high_count, " +
            "SUM(CASE WHEN quantity > minimum_stock AND quantity <= target_stock AND status != 'EXPIRED' AND expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END) as moderate_count, " +
            "SUM(CASE WHEN quantity > target_stock AND status != 'EXPIRED' AND expiry_date >= CURRENT_DATE THEN 1 ELSE 0 END) as low_count " +
            "FROM inventory_batches " +
            "WHERE status != 'DEPLETED' "
        );
        
        List<Object> params = new ArrayList<>();
        if (facilityId != null) {
            sql.append("AND facility_id = ? ");
            params.add(facilityId);
        }

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return result.isEmpty() ? null : result.get(0);
    }
}
