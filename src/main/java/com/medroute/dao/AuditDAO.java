package com.medroute.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void log(Long userId, String action, String entityType, String entityId,
                    String oldValue, String newValue, String ipAddress, String userAgent) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, action, entityType, entityId, oldValue, newValue, ipAddress, userAgent);
    }
}
