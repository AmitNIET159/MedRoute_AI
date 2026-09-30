package com.medroute.dao;

import com.medroute.model.StockConsumption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class StockConsumptionDAO {

    private final JdbcTemplate jdbcTemplate;

    public StockConsumptionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(StockConsumption consumption) {
        String sql = "INSERT INTO stock_consumption (facility_id, medicine_id, quantity, department, consumed_by, recorded_by, reference_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, consumption.getFacilityId());
            ps.setLong(2, consumption.getMedicineId());
            ps.setInt(3, consumption.getQuantity());
            ps.setString(4, consumption.getDepartment());
            ps.setString(5, consumption.getConsumedBy());
            ps.setObject(6, consumption.getRecordedBy());
            ps.setString(7, consumption.getReferenceId());
            ps.setString(8, consumption.getNotes());
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }
}
