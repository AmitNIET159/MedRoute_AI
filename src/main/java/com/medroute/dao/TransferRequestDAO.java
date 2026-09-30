package com.medroute.dao;

import com.medroute.model.TransferRequest;
import com.medroute.model.TransferStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransferRequestDAO {
    private final JdbcTemplate jdbcTemplate;

    public TransferRequestDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TransferRequest> rowMapper = (rs, rowNum) -> {
        TransferRequest tr = new TransferRequest();
        tr.setId(rs.getLong("id"));
        tr.setFromFacilityId(rs.getLong("from_facility_id"));
        tr.setToFacilityId(rs.getLong("to_facility_id"));
        tr.setEmergencyRequestId(rs.getObject("emergency_request_id", Long.class));
        tr.setRequestedQuantity(rs.getInt("requested_quantity"));
        tr.setStatus(TransferStatus.valueOf(rs.getString("status")));
        tr.setMatchScore(rs.getBigDecimal("match_score"));
        tr.setDistanceKm(rs.getBigDecimal("distance_km"));
        tr.setNotes(rs.getString("notes"));
        long requestedBy = rs.getLong("requested_by");
        if (!rs.wasNull()) tr.setRequestedBy(requestedBy);
        long approvedBy = rs.getLong("approved_by");
        if (!rs.wasNull()) tr.setApprovedBy(approvedBy);
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) tr.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) tr.setUpdatedAt(updatedAt.toLocalDateTime());

        try { tr.setFromFacilityName(rs.getString("from_facility_name")); } catch (Exception e) {}
        try { tr.setToFacilityName(rs.getString("to_facility_name")); } catch (Exception e) {}
        try { tr.setRequestedByName(rs.getString("requested_by_name")); } catch (Exception e) {}
        try { tr.setApprovedByName(rs.getString("approved_by_name")); } catch (Exception e) {}
        try { tr.setMedicineName(rs.getString("medicine_name")); } catch (Exception e) {}

        return tr;
    };

    private static final String SELECT_WITH_JOINS = 
        "SELECT tr.*, " +
        "  ff.name AS from_facility_name, " +
        "  tf.name AS to_facility_name, " +
        "  ru.full_name AS requested_by_name, " +
        "  au.full_name AS approved_by_name, " +
        "  m.name AS medicine_name " +
        "FROM transfer_requests tr " +
        "LEFT JOIN facilities ff ON tr.from_facility_id = ff.id " +
        "LEFT JOIN facilities tf ON tr.to_facility_id = tf.id " +
        "LEFT JOIN users ru ON tr.requested_by = ru.id " +
        "LEFT JOIN users au ON tr.approved_by = au.id " +
        "LEFT JOIN emergency_requests er ON tr.emergency_request_id = er.id " +
        "LEFT JOIN medicine_catalog m ON er.medicine_id = m.id ";

    public Long create(TransferRequest tr) {
        String sql = "INSERT INTO transfer_requests (from_facility_id, to_facility_id, emergency_request_id, " +
                     "requested_quantity, status, match_score, distance_km, notes, requested_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tr.getFromFacilityId());
            ps.setLong(2, tr.getToFacilityId());
            ps.setObject(3, tr.getEmergencyRequestId());
            ps.setInt(4, tr.getRequestedQuantity());
            ps.setString(5, tr.getStatus().name());
            ps.setBigDecimal(6, tr.getMatchScore());
            ps.setBigDecimal(7, tr.getDistanceKm());
            ps.setString(8, tr.getNotes());
            ps.setObject(9, tr.getRequestedBy());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<TransferRequest> findById(Long id) {
        String sql = SELECT_WITH_JOINS + " WHERE tr.id = ?";
        List<TransferRequest> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private void appendStatusFilter(StringBuilder sql, List<Object> params, String statusFilter) {
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND tr.status = ? ");
            params.add(statusFilter);
        }
    }

    public List<TransferRequest> findByFacilityId(Long facilityId, int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE (tr.from_facility_id = ? OR tr.to_facility_id = ?) ");
        params.add(facilityId);
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        sql.append(" ORDER BY tr.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<TransferRequest> findIncoming(Long facilityId, int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE tr.from_facility_id = ? ");
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        sql.append(" ORDER BY tr.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<TransferRequest> findOutgoing(Long facilityId, int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE tr.to_facility_id = ? ");
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        sql.append(" ORDER BY tr.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<TransferRequest> findAll(int offset, int limit, String statusFilter) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_JOINS);
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE 1=1 ");
        appendStatusFilter(sql, params, statusFilter);
        sql.append(" ORDER BY tr.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public int countByFacilityId(Long facilityId, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transfer_requests tr WHERE (tr.from_facility_id = ? OR tr.to_facility_id = ?) ");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    public int countIncoming(Long facilityId, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transfer_requests tr WHERE tr.from_facility_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    public int countOutgoing(Long facilityId, String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transfer_requests tr WHERE tr.to_facility_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(facilityId);
        appendStatusFilter(sql, params, statusFilter);
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    public int countAll(String statusFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transfer_requests tr WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendStatusFilter(sql, params, statusFilter);
        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    public void updateStatus(Long id, TransferStatus status) {
        String sql = "UPDATE transfer_requests SET status = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    public void updateApprovedBy(Long id, Long approvedBy) {
        String sql = "UPDATE transfer_requests SET approved_by = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, approvedBy, id);
    }

    public void updateNotes(Long id, String notes) {
        String sql = "UPDATE transfer_requests SET notes = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, notes, id);
    }

    public List<TransferRequest> findByEmergencyRequestId(Long emergencyRequestId) {
        String sql = SELECT_WITH_JOINS + " WHERE tr.emergency_request_id = ? ORDER BY tr.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, emergencyRequestId);
    }
}
