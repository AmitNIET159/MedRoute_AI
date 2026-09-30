package com.medroute.dao;

import com.medroute.model.BatchStatus;
import com.medroute.model.InventoryBatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InventoryBatchDAO {

    private final JdbcTemplate jdbcTemplate;

    public InventoryBatchDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<InventoryBatch> rowMapper = (rs, rowNum) -> {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(rs.getLong("id"));
        batch.setFacilityId(rs.getLong("facility_id"));
        batch.setMedicineId(rs.getLong("medicine_id"));
        batch.setBatchNumber(rs.getString("batch_number"));
        batch.setQuantity(rs.getInt("quantity"));
        batch.setReservedQuantity(rs.getInt("reserved_quantity"));
        batch.setMinimumStock(rs.getInt("minimum_stock"));
        batch.setTargetStock(rs.getInt("target_stock"));
        batch.setUnitPrice(rs.getBigDecimal("unit_price"));
        
        Date mfg = rs.getDate("manufacture_date");
        if (mfg != null) batch.setManufactureDate(mfg.toLocalDate());
        
        Date exp = rs.getDate("expiry_date");
        if (exp != null) batch.setExpiryDate(exp.toLocalDate());
        
        Date rcv = rs.getDate("received_date");
        if (rcv != null) batch.setReceivedDate(rcv.toLocalDate());
        
        batch.setSupplier(rs.getString("supplier"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            batch.setStatus(BatchStatus.valueOf(statusStr));
        }
        
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) batch.setCreatedAt(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) batch.setUpdatedAt(updated.toLocalDateTime());
        
        return batch;
    };

    public Long create(InventoryBatch batch) {
        String sql = "INSERT INTO inventory_batches (facility_id, medicine_id, batch_number, quantity, reserved_quantity, minimum_stock, target_stock, unit_price, manufacture_date, expiry_date, received_date, supplier, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, batch.getFacilityId());
            ps.setLong(2, batch.getMedicineId());
            ps.setString(3, batch.getBatchNumber());
            ps.setInt(4, batch.getQuantity());
            ps.setInt(5, batch.getReservedQuantity());
            ps.setInt(6, batch.getMinimumStock());
            ps.setInt(7, batch.getTargetStock());
            ps.setBigDecimal(8, batch.getUnitPrice());
            ps.setDate(9, batch.getManufactureDate() != null ? Date.valueOf(batch.getManufactureDate()) : null);
            ps.setDate(10, batch.getExpiryDate() != null ? Date.valueOf(batch.getExpiryDate()) : null);
            ps.setDate(11, batch.getReceivedDate() != null ? Date.valueOf(batch.getReceivedDate()) : null);
            ps.setString(12, batch.getSupplier());
            ps.setString(13, batch.getStatus() != null ? batch.getStatus().name() : BatchStatus.ACTIVE.name());
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void update(InventoryBatch batch) {
        String sql = "UPDATE inventory_batches SET quantity = ?, reserved_quantity = ?, minimum_stock = ?, target_stock = ?, unit_price = ?, manufacture_date = ?, expiry_date = ?, received_date = ?, supplier = ?, status = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                batch.getQuantity(),
                batch.getReservedQuantity(),
                batch.getMinimumStock(),
                batch.getTargetStock(),
                batch.getUnitPrice(),
                batch.getManufactureDate() != null ? Date.valueOf(batch.getManufactureDate()) : null,
                batch.getExpiryDate() != null ? Date.valueOf(batch.getExpiryDate()) : null,
                batch.getReceivedDate() != null ? Date.valueOf(batch.getReceivedDate()) : null,
                batch.getSupplier(),
                batch.getStatus() != null ? batch.getStatus().name() : null,
                batch.getId());
    }

    public InventoryBatch findById(Long id) {
        String sql = "SELECT * FROM inventory_batches WHERE id = ?";
        List<InventoryBatch> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<InventoryBatch> findByFacilityId(Long facilityId, int offset, int limit, String search, Long categoryId, String status, String expiryRange) {
        StringBuilder sql = new StringBuilder("SELECT b.*, m.name as medicine_name FROM inventory_batches b JOIN medicine_catalog m ON b.medicine_id = m.id WHERE b.facility_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);

        if (search != null && !search.isEmpty()) {
            sql.append(" AND m.name ILIKE ?");
            params.add("%" + search + "%");
        }
        if (categoryId != null) {
            sql.append(" AND m.category_id = ?");
            params.add(categoryId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND b.status = ?");
            params.add(status);
        }
        if ("EXPIRING_SOON".equalsIgnoreCase(expiryRange)) {
            sql.append(" AND b.expiry_date >= ? AND b.expiry_date <= ?");
            params.add(Date.valueOf(LocalDate.now()));
            params.add(Date.valueOf(LocalDate.now().plusDays(30)));
        } else if ("EXPIRED".equalsIgnoreCase(expiryRange)) {
            sql.append(" AND b.expiry_date < ?");
            params.add(Date.valueOf(LocalDate.now()));
        }

        sql.append(" ORDER BY b.expiry_date ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            InventoryBatch batch = rowMapper.mapRow(rs, rowNum);
            batch.setMedicineName(rs.getString("medicine_name"));
            return batch;
        }, params.toArray());
    }

    public int countByFacilityId(Long facilityId, String search, Long categoryId, String status, String expiryRange) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM inventory_batches b JOIN medicine_catalog m ON b.medicine_id = m.id WHERE b.facility_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);

        if (search != null && !search.isEmpty()) {
            sql.append(" AND m.name ILIKE ?");
            params.add("%" + search + "%");
        }
        if (categoryId != null) {
            sql.append(" AND m.category_id = ?");
            params.add(categoryId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND b.status = ?");
            params.add(status);
        }
        if ("EXPIRING_SOON".equalsIgnoreCase(expiryRange)) {
            sql.append(" AND b.expiry_date >= ? AND b.expiry_date <= ?");
            params.add(Date.valueOf(LocalDate.now()));
            params.add(Date.valueOf(LocalDate.now().plusDays(30)));
        } else if ("EXPIRED".equalsIgnoreCase(expiryRange)) {
            sql.append(" AND b.expiry_date < ?");
            params.add(Date.valueOf(LocalDate.now()));
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<InventoryBatch> findLowStock(Long facilityId) {
        String sql = "SELECT * FROM inventory_batches WHERE facility_id = ? AND quantity <= minimum_stock AND status != 'DEPLETED'";
        return jdbcTemplate.query(sql, rowMapper, facilityId);
    }

    public List<InventoryBatch> findExpiring(Long facilityId) {
        String sql = "SELECT * FROM inventory_batches WHERE facility_id = ? AND expiry_date <= ? AND status != 'DEPLETED'";
        return jdbcTemplate.query(sql, rowMapper, facilityId, Date.valueOf(LocalDate.now().plusDays(30)));
    }

    /**
     * Selects active, non-expired batches for a facility+medicine with row-level locks.
     * Must be called within a @Transactional context.
     * Used by TransferService for concurrency-safe reservation and dispatch.
     */
    public List<InventoryBatch> findForUpdateByFacilityAndMedicine(Long facilityId, Long medicineId) {
        String sql = "SELECT * FROM inventory_batches " +
                     "WHERE facility_id = ? AND medicine_id = ? AND status = 'ACTIVE' " +
                     "AND expiry_date > CURRENT_DATE " +
                     "ORDER BY expiry_date ASC " +
                     "FOR UPDATE";
        return jdbcTemplate.query(sql, rowMapper, facilityId, medicineId);
    }
}
