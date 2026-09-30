package com.medroute.dao;

import com.medroute.model.Facility;
import com.medroute.model.FacilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FacilityDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Facility> FACILITY_ROW_MAPPER = (rs, rowNum) -> {
        Facility facility = new Facility();
        facility.setId(rs.getLong("id"));
        facility.setName(rs.getString("name"));
        facility.setFacilityType(FacilityType.valueOf(rs.getString("facility_type")));
        facility.setRegistrationNumber(rs.getString("registration_number"));
        facility.setAddress(rs.getString("address"));
        facility.setCity(rs.getString("city"));
        facility.setState(rs.getString("state"));
        facility.setPincode(rs.getString("pincode"));
        facility.setPhone(rs.getString("phone"));
        facility.setEmail(rs.getString("email"));
        facility.setLatitude(rs.getBigDecimal("latitude"));
        facility.setLongitude(rs.getBigDecimal("longitude"));
        facility.setActive(rs.getBoolean("is_active"));
        facility.setReliabilityScore(rs.getBigDecimal("reliability_score"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            facility.setCreatedAt(createdAt.toLocalDateTime());
        }
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            facility.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return facility;
    };

    /** Returns all active facilities, ordered by name, for registration dropdown. */
    public List<Facility> findAllActive() {
        String sql = "SELECT * FROM facilities WHERE is_active = true ORDER BY name ASC";
        return jdbcTemplate.query(sql, FACILITY_ROW_MAPPER);
    }

    public Optional<Facility> findById(Long id) {
        String sql = "SELECT * FROM facilities WHERE id = ?";
        List<Facility> facilities = jdbcTemplate.query(sql, FACILITY_ROW_MAPPER, id);
        return facilities.isEmpty() ? Optional.empty() : Optional.of(facilities.get(0));
    }

    public void create(Facility facility) {
        String sql = "INSERT INTO facilities (name, facility_type, registration_number, address, city, state, pincode, phone, email, latitude, longitude, is_active, reliability_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, 
            facility.getName(), 
            facility.getFacilityType() != null ? facility.getFacilityType().name() : null, 
            facility.getRegistrationNumber(), 
            facility.getAddress(), 
            facility.getCity(), 
            facility.getState(), 
            facility.getPincode(), 
            facility.getPhone(), 
            facility.getEmail(), 
            facility.getLatitude(), 
            facility.getLongitude(), 
            facility.isActive(), 
            facility.getReliabilityScore());
    }

    public void update(Facility facility) {
        String sql = "UPDATE facilities SET name = ?, facility_type = ?, registration_number = ?, address = ?, city = ?, state = ?, pincode = ?, phone = ?, email = ?, latitude = ?, longitude = ?, is_active = ?, reliability_score = ? WHERE id = ?";
        jdbcTemplate.update(sql, 
            facility.getName(), 
            facility.getFacilityType() != null ? facility.getFacilityType().name() : null, 
            facility.getRegistrationNumber(), 
            facility.getAddress(), 
            facility.getCity(), 
            facility.getState(), 
            facility.getPincode(), 
            facility.getPhone(), 
            facility.getEmail(), 
            facility.getLatitude(), 
            facility.getLongitude(), 
            facility.isActive(), 
            facility.getReliabilityScore(), 
            facility.getId());
    }

    public List<Facility> findAll(int offset, int limit) {
        String sql = "SELECT * FROM facilities ORDER BY id ASC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, FACILITY_ROW_MAPPER, limit, offset);
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM facilities";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public void updateCoordinates(Long facilityId, Double latitude, Double longitude) {
        String sql = "UPDATE facilities SET latitude = ?, longitude = ? WHERE id = ?";
        jdbcTemplate.update(sql, latitude, longitude, facilityId);
    }

    public List<Facility> findActiveBounded(int limit) {
        String sql = "SELECT * FROM facilities WHERE is_active = true ORDER BY id ASC LIMIT ?";
        return jdbcTemplate.query(sql, FACILITY_ROW_MAPPER, limit);
    }
}
