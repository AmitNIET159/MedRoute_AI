package com.medroute.dao;

import com.medroute.model.EmergencyRequest;
import com.medroute.model.RequestStatus;
import com.medroute.model.Urgency;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EmergencyRequestDAO {

    private final JdbcTemplate jdbcTemplate;

    public EmergencyRequestDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper that joins facility name, medicine name, and creator name
     * for display-ready EmergencyRequest objects.
     */
    private static final RowMapper<EmergencyRequest> REQUEST_ROW_MAPPER = (rs, rowNum) -> {
        EmergencyRequest r = new EmergencyRequest();
        r.setId(rs.getLong("id"));
        r.setRequestingFacilityId(rs.getLong("requesting_facility_id"));
        r.setMedicineId(rs.getLong("medicine_id"));
        r.setQuantityNeeded(rs.getInt("quantity_needed"));
        r.setQuantityFulfilled(rs.getInt("quantity_fulfilled"));
        r.setUrgency(Urgency.valueOf(rs.getString("urgency")));
        Date reqBy = rs.getDate("required_by_date");
        if (reqBy != null) r.setRequiredByDate(reqBy.toLocalDate());
        r.setReason(rs.getString("reason"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setAiPriorityScore(rs.getBigDecimal("ai_priority_score"));
        r.setCreatedBy(rs.getLong("created_by"));
        if (rs.wasNull()) r.setCreatedBy(null);
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

        // Joined columns (may not exist in all queries)
        try { r.setFacilityName(rs.getString("facility_name")); } catch (Exception ignored) {}
        try { r.setMedicineName(rs.getString("medicine_name")); } catch (Exception ignored) {}
        try { r.setCreatedByName(rs.getString("created_by_name")); } catch (Exception ignored) {}

        return r;
    };

    private static final String SELECT_WITH_JOINS =
            "SELECT er.*, " +
            "f.name AS facility_name, " +
            "m.name AS medicine_name, " +
            "u.full_name AS created_by_name " +
            "FROM emergency_requests er " +
            "LEFT JOIN facilities f ON er.requesting_facility_id = f.id " +
            "LEFT JOIN medicine_catalog m ON er.medicine_id = m.id " +
            "LEFT JOIN users u ON er.created_by = u.id ";

    public Long create(EmergencyRequest request) {
        String sql = "INSERT INTO emergency_requests " +
                "(requesting_facility_id, medicine_id, quantity_needed, quantity_fulfilled, " +
                "urgency, required_by_date, reason, status, ai_priority_score, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getRequestingFacilityId());
            ps.setLong(2, request.getMedicineId());
            ps.setInt(3, request.getQuantityNeeded());
            ps.setInt(4, request.getQuantityFulfilled());
            ps.setString(5, request.getUrgency().name());
            ps.setDate(6, Date.valueOf(request.getRequiredByDate()));
            ps.setString(7, request.getReason());
            ps.setString(8, request.getStatus().name());
            if (request.getAiPriorityScore() != null) {
                ps.setBigDecimal(9, request.getAiPriorityScore());
            } else {
                ps.setNull(9, java.sql.Types.DECIMAL);
            }
            if (request.getCreatedBy() != null) {
                ps.setLong(10, request.getCreatedBy());
            } else {
                ps.setNull(10, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<EmergencyRequest> findById(Long id) {
        String sql = SELECT_WITH_JOINS + "WHERE er.id = ?";
        List<EmergencyRequest> results = jdbcTemplate.query(sql, REQUEST_ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<EmergencyRequest> findByFacilityId(Long facilityId, int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE er.requesting_facility_id = ? ");
        params.add(facilityId);
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND er.status = ? ");
            params.add(statusFilter);
        }
        sql.append("ORDER BY er.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), REQUEST_ROW_MAPPER, params.toArray());
    }

    public List<EmergencyRequest> findAll(int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append("WHERE 1=1 ");
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND er.status = ? ");
            params.add(statusFilter);
        }
        sql.append("ORDER BY er.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), REQUEST_ROW_MAPPER, params.toArray());
    }

    public int countByFacilityId(Long facilityId, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM emergency_requests WHERE requesting_facility_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND status = ? ");
            params.add(statusFilter);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public int countAll(String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM emergency_requests WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND status = ? ");
            params.add(statusFilter);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public void updateStatus(Long id, RequestStatus status) {
        jdbcTemplate.update("UPDATE emergency_requests SET status = ? WHERE id = ?", status.name(), id);
    }

    public void updateQuantityFulfilled(Long id, int quantityFulfilled) {
        jdbcTemplate.update("UPDATE emergency_requests SET quantity_fulfilled = ? WHERE id = ?",
                quantityFulfilled, id);
    }

    public int countOpenByFacilityId(Long facilityId) {
        String sql = "SELECT COUNT(*) FROM emergency_requests " +
                     "WHERE requesting_facility_id = ? AND status IN ('OPEN', 'PARTIALLY_FULFILLED')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, facilityId);
        return count != null ? count : 0;
    }
}
