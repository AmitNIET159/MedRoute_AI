package com.medroute.dao;

import com.medroute.model.InventoryTransaction;
import com.medroute.model.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class InventoryTransactionDAO {

    private final JdbcTemplate jdbcTemplate;

    public InventoryTransactionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<InventoryTransaction> rowMapper = (rs, rowNum) -> {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setId(rs.getLong("id"));
        tx.setBatchId(rs.getLong("batch_id"));
        tx.setFacilityId(rs.getLong("facility_id"));
        tx.setMedicineId(rs.getLong("medicine_id"));
        
        String typeStr = rs.getString("transaction_type");
        if (typeStr != null) {
            tx.setTransactionType(TransactionType.valueOf(typeStr));
        }
        
        tx.setQuantity(rs.getInt("quantity"));
        tx.setPerformedBy(rs.getLong("performed_by"));
        tx.setReferenceId(rs.getString("reference_id"));
        tx.setNotes(rs.getString("notes"));
        
        Timestamp date = rs.getTimestamp("transaction_date");
        if (date != null) tx.setTransactionDate(date.toLocalDateTime());
        
        return tx;
    };

    public Long create(InventoryTransaction tx) {
        String sql = "INSERT INTO inventory_transactions (batch_id, facility_id, medicine_id, transaction_type, quantity, performed_by, reference_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, tx.getBatchId());
            ps.setLong(2, tx.getFacilityId());
            ps.setLong(3, tx.getMedicineId());
            ps.setString(4, tx.getTransactionType() != null ? tx.getTransactionType().name() : null);
            ps.setInt(5, tx.getQuantity());
            ps.setObject(6, tx.getPerformedBy());
            ps.setString(7, tx.getReferenceId());
            ps.setString(8, tx.getNotes());
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public List<InventoryTransaction> findByBatchId(Long batchId) {
        String sql = "SELECT * FROM inventory_transactions WHERE batch_id = ? ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, batchId);
    }

    public List<InventoryTransaction> findByFacilityId(Long facilityId) {
        String sql = "SELECT * FROM inventory_transactions WHERE facility_id = ? ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, facilityId);
    }
}
