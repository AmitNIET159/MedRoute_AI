package com.medroute.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medroute.model.AIInsight;
import com.medroute.model.AIUsageLog;
import com.medroute.model.InsightType;
import com.medroute.model.Severity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AIDAO {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AIDAO(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private final RowMapper<AIInsight> insightRowMapper = (rs, rowNum) -> {
        AIInsight insight = new AIInsight();
        insight.setId(rs.getLong("id"));
        insight.setFacilityId(rs.getLong("facility_id"));
        insight.setMedicineId(rs.getLong("medicine_id"));
        
        String typeStr = rs.getString("insight_type");
        if (typeStr != null) insight.setInsightType(InsightType.valueOf(typeStr));
        
        insight.setTitle(rs.getString("title"));
        insight.setContent(rs.getString("content"));
        
        String sevStr = rs.getString("severity");
        if (sevStr != null) insight.setSeverity(Severity.valueOf(sevStr));
        
        insight.setRiskScore(rs.getBigDecimal("risk_score"));
        insight.setMetadata(rs.getString("metadata"));
        insight.setRequestHash(rs.getString("request_hash"));
        insight.setRead(rs.getBoolean("is_read"));
        
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) insight.setCreatedAt(created.toLocalDateTime());
        
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) insight.setExpiresAt(expires.toLocalDateTime());
        
        return insight;
    };

    public Long createInsight(AIInsight insight) {
        String sql = "INSERT INTO ai_insights (facility_id, medicine_id, insight_type, title, content, severity, risk_score, metadata, request_hash, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, insight.getFacilityId());
            ps.setObject(2, insight.getMedicineId());
            ps.setString(3, insight.getInsightType().name());
            ps.setString(4, insight.getTitle());
            ps.setString(5, insight.getContent());
            ps.setString(6, insight.getSeverity().name());
            ps.setObject(7, insight.getRiskScore());
            ps.setString(8, insight.getMetadata());
            ps.setString(9, insight.getRequestHash());
            ps.setTimestamp(10, insight.getExpiresAt() != null ? Timestamp.valueOf(insight.getExpiresAt()) : null);
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public Optional<AIInsight> findCachedInsight(String requestHash) {
        String sql = "SELECT * FROM ai_insights WHERE request_hash = ? AND (expires_at IS NULL OR expires_at > NOW()) ORDER BY created_at DESC LIMIT 1";
        List<AIInsight> results = jdbcTemplate.query(sql, insightRowMapper, requestHash);
        return results.stream().findFirst();
    }

    public Optional<AIInsight> findByRequestHash(String requestHash) {
        String sql = "SELECT * FROM ai_insights WHERE request_hash = ? ORDER BY created_at DESC LIMIT 1";
        List<AIInsight> results = jdbcTemplate.query(sql, insightRowMapper, requestHash);
        return results.stream().findFirst();
    }

    public List<AIInsight> findByFacility(Long facilityId, int limit) {
        String sql = "SELECT * FROM ai_insights WHERE facility_id = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, insightRowMapper, facilityId, limit);
    }

    public void logUsage(AIUsageLog log) {
        String sql = "INSERT INTO ai_usage_logs (user_id, request_type, model_used, input_tokens, output_tokens, response_time_ms, is_cached, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, 
            log.getUserId(), 
            log.getRequestType(), 
            log.getModelUsed(), 
            log.getInputTokens(), 
            log.getOutputTokens(), 
            log.getResponseTimeMs(), 
            log.getIsCached(), 
            log.getStatus());
    }
}
