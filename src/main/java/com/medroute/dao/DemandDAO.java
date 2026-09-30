package com.medroute.dao;

import com.medroute.model.InventoryBatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public class DemandDAO {

    private final JdbcTemplate jdbcTemplate;

    public DemandDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getConsumptionInPeriod(Long facilityId, Long medicineId, LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM inventory_transactions " +
                     "WHERE facility_id = ? AND medicine_id = ? " +
                     "AND transaction_type = 'CONSUMPTION' " +
                     "AND created_at >= ? AND created_at < ?";
        
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class, facilityId, medicineId, from, to.plusDays(1));
        return sum != null ? sum : 0;
    }

    public boolean hasAnyConsumptionData(Long facilityId, Long medicineId) {
        String sql = "SELECT COUNT(id) FROM inventory_transactions " +
                     "WHERE facility_id = ? AND medicine_id = ? " +
                     "AND transaction_type = 'CONSUMPTION'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, facilityId, medicineId);
        return count != null && count > 0;
    }

    public int countActiveEmergencyRequests(Long facilityId, Long medicineId) {
        String sql = "SELECT COUNT(id) FROM emergency_requests " +
                     "WHERE requesting_facility_id = ? AND medicine_id = ? " +
                     "AND status IN ('OPEN', 'PARTIALLY_FULFILLED')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, facilityId, medicineId);
        return count != null ? count : 0;
    }

    private final RowMapper<InventoryBatch> batchRowMapper = (rs, rowNum) -> {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(rs.getLong("id"));
        batch.setFacilityId(rs.getLong("facility_id"));
        batch.setMedicineId(rs.getLong("medicine_id"));
        batch.setBatchNumber(rs.getString("batch_number"));
        batch.setQuantity(rs.getInt("quantity"));
        batch.setReservedQuantity(rs.getInt("reserved_quantity"));
        
        Date exp = rs.getDate("expiry_date");
        if (exp != null) batch.setExpiryDate(exp.toLocalDate());
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            batch.setStatus(com.medroute.model.BatchStatus.valueOf(statusStr));
        }
        
        return batch;
    };

    public List<InventoryBatch> getActiveBatches(Long facilityId, Long medicineId) {
        String sql = "SELECT * FROM inventory_batches " +
                     "WHERE facility_id = ? AND medicine_id = ? " +
                     "AND status IN ('ACTIVE', 'EXPIRED')";
        return jdbcTemplate.query(sql, batchRowMapper, facilityId, medicineId);
    }
}
