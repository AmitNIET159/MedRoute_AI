package com.medroute.dao;

import com.medroute.model.GeocodingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class GeocodingCacheDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<GeocodingResult> ROW_MAPPER = (rs, rowNum) -> {
        GeocodingResult result = new GeocodingResult();
        result.setLatitude(rs.getDouble("latitude"));
        result.setLongitude(rs.getDouble("longitude"));
        result.setDisplayName(rs.getString("display_name"));
        result.setAddressJson(rs.getString("address_json"));
        return result;
    };

    public Optional<GeocodingResult> findByQueryHash(String queryHash) {
        String sql = "SELECT latitude, longitude, display_name, address_json FROM geocoding_cache WHERE query_hash = ? AND expires_at > CURRENT_TIMESTAMP";
        List<GeocodingResult> results = jdbcTemplate.query(sql, ROW_MAPPER, queryHash);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void save(String queryHash, String queryText, GeocodingResult result, int ttlDays) {
        String sql = "INSERT INTO geocoding_cache (query_hash, query_text, latitude, longitude, display_name, address_json, created_at, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?) " +
                     "ON DUPLICATE KEY UPDATE latitude = VALUES(latitude), longitude = VALUES(longitude), " +
                     "display_name = VALUES(display_name), address_json = VALUES(address_json), " +
                     "created_at = CURRENT_TIMESTAMP, expires_at = VALUES(expires_at)";
        
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(ttlDays);
        jdbcTemplate.update(sql, queryHash, queryText, result.getLatitude(), result.getLongitude(), 
                            result.getDisplayName(), result.getAddressJson(), Timestamp.valueOf(expiresAt));
    }
}
