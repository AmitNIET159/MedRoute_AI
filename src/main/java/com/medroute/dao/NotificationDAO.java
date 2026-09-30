package com.medroute.dao;

import com.medroute.model.Notification;
import com.medroute.model.NotificationType;
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
public class NotificationDAO {
    private final JdbcTemplate jdbcTemplate;

    public NotificationDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> rowMapper = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getObject("user_id", Long.class));
        n.setFacilityId(rs.getObject("facility_id", Long.class));
        n.setType(NotificationType.valueOf(rs.getString("type")));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setReferenceId(rs.getString("reference_id"));
        n.setReferenceType(rs.getString("reference_type"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) n.setCreatedAt(createdAt.toLocalDateTime());
        return n;
    };

    public Long create(Notification n) {
        String sql = "INSERT INTO notifications (user_id, facility_id, type, title, message, is_read, reference_id, reference_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, n.getUserId());
            ps.setObject(2, n.getFacilityId());
            ps.setString(3, n.getType().name());
            ps.setString(4, n.getTitle());
            ps.setString(5, n.getMessage());
            ps.setBoolean(6, n.isRead());
            ps.setString(7, n.getReferenceId());
            ps.setString(8, n.getReferenceType());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<Notification> findByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, userId, limit);
    }

    public List<Notification> findUnreadByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, userId, limit);
    }

    public int countUnreadByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        jdbcTemplate.update(sql, notificationId, userId);
    }

    public void markAllReadByUserId(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
}
