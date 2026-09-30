package com.medroute.dao;

import com.medroute.model.TransferItem;
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
public class TransferItemDAO {
    private final JdbcTemplate jdbcTemplate;

    public TransferItemDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TransferItem> rowMapper = (rs, rowNum) -> {
        TransferItem item = new TransferItem();
        item.setId(rs.getLong("id"));
        item.setTransferId(rs.getLong("transfer_id"));
        item.setBatchId(rs.getLong("batch_id"));
        item.setMedicineId(rs.getLong("medicine_id"));
        item.setQuantity(rs.getInt("quantity"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) item.setCreatedAt(createdAt.toLocalDateTime());

        try { item.setBatchNumber(rs.getString("batch_number")); } catch (Exception e) {}
        try { item.setMedicineName(rs.getString("medicine_name")); } catch (Exception e) {}

        return item;
    };

    public Long create(TransferItem item) {
        String sql = "INSERT INTO transfer_items (transfer_id, batch_id, medicine_id, quantity) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getTransferId());
            ps.setLong(2, item.getBatchId());
            ps.setLong(3, item.getMedicineId());
            ps.setInt(4, item.getQuantity());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<TransferItem> findByTransferId(Long transferId) {
        String sql = "SELECT ti.*, ib.batch_number, m.name AS medicine_name " +
                     "FROM transfer_items ti " +
                     "LEFT JOIN inventory_batches ib ON ti.batch_id = ib.id " +
                     "LEFT JOIN medicine_catalog m ON ti.medicine_id = m.id " +
                     "WHERE ti.transfer_id = ?";
        return jdbcTemplate.query(sql, rowMapper, transferId);
    }
}
