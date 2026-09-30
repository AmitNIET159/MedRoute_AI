package com.medroute.dao;

import com.medroute.model.OTPPurpose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OTPDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public record OTPRecord(
            Long id,
            String email,
            String otpHash,
            String purpose,
            int attempts,
            boolean used,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {}

    private static final RowMapper<OTPRecord> OTP_ROW_MAPPER = (rs, rowNum) -> new OTPRecord(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("otp_hash"),
            rs.getString("purpose"),
            rs.getInt("attempts"),
            rs.getBoolean("is_used"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toLocalDateTime() : null
    );

    public void create(String email, String otpHash, String purpose, LocalDateTime expiresAt) {
        String sql = "INSERT INTO otp_verifications (email, otp_hash, purpose, expires_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, email, otpHash, purpose, expiresAt);
    }

    public void invalidatePrevious(String email, String purpose) {
        String sql = "UPDATE otp_verifications SET is_used = true WHERE email = ? AND purpose = ? AND is_used = false";
        jdbcTemplate.update(sql, email, purpose);
    }

    public Optional<OTPRecord> findLatestValid(String email, String purpose) {
        String sql = "SELECT * FROM otp_verifications WHERE email = ? AND purpose = ? AND is_used = false " +
                     "AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1";
        List<OTPRecord> records = jdbcTemplate.query(sql, OTP_ROW_MAPPER, email, purpose);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    public void incrementAttempts(Long otpId) {
        String sql = "UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = ?";
        jdbcTemplate.update(sql, otpId);
    }

    public void markUsed(Long otpId) {
        String sql = "UPDATE otp_verifications SET is_used = true WHERE id = ?";
        jdbcTemplate.update(sql, otpId);
    }

    public int countRecentRequests(String email, int minutes) {
        String sql = "SELECT COUNT(*) FROM otp_verifications WHERE email = ? AND created_at >= (NOW() - INTERVAL ? MINUTE)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, minutes);
        return count != null ? count : 0;
    }
}
