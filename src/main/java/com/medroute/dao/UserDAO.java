package com.medroute.dao;

import com.medroute.model.Role;
import com.medroute.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDAO {
    
    @Autowired 
    private JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setRole(Role.valueOf(rs.getString("role")));
        
        long facilityId = rs.getLong("facility_id");
        if (!rs.wasNull()) {
            user.setFacilityId(facilityId);
        }
        
        user.setActive(rs.getBoolean("is_active"));
        user.setVerified(rs.getBoolean("is_verified"));
        
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    };
    
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, USER_ROW_MAPPER, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }
    
    public Long create(User user) {
        String sql = "INSERT INTO users (email, password_hash, full_name, phone, role, facility_id, is_active, is_verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            
            if (user.getFacilityId() != null) {
                ps.setLong(6, user.getFacilityId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            
            ps.setBoolean(7, user.isActive());
            ps.setBoolean(8, user.isVerified());
            return ps;
        }, keyHolder);
        
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }
    
    public void updateVerified(Long userId, boolean verified) {
        String sql = "UPDATE users SET is_verified = ? WHERE id = ?";
        jdbcTemplate.update(sql, verified, userId);
    }
    
    public void updatePassword(Long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        jdbcTemplate.update(sql, passwordHash, userId);
    }
    
    public void updateActive(Long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, active, userId);
    }
    
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public List<User> findActiveByFacilityId(Long facilityId) {
        String sql = "SELECT * FROM users WHERE facility_id = ? AND is_active = TRUE";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, facilityId);
    }
}
