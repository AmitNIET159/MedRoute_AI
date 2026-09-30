package com.medroute.dao;

import com.medroute.model.WeatherSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class WeatherCacheDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<WeatherSnapshot> ROW_MAPPER = (rs, rowNum) -> {
        WeatherSnapshot snapshot = new WeatherSnapshot();
        snapshot.setLatitude(rs.getDouble("latitude"));
        snapshot.setLongitude(rs.getDouble("longitude"));
        snapshot.setTemperature(rs.getDouble("temperature"));
        snapshot.setHumidity(rs.getDouble("humidity"));
        snapshot.setRainProbability(rs.getDouble("rain_probability"));
        snapshot.setWindSpeed(rs.getDouble("wind_speed"));
        snapshot.setWeatherCode(rs.getInt("weather_code"));
        snapshot.setLogisticsContext(rs.getString("logistics_context"));
        Timestamp retrievedAt = rs.getTimestamp("retrieved_at");
        if (retrievedAt != null) {
            snapshot.setRetrievedAt(retrievedAt.toLocalDateTime());
        }
        return snapshot;
    };

    public Optional<WeatherSnapshot> findByLocationHash(String locationHash) {
        String sql = "SELECT * FROM weather_cache WHERE location_hash = ? AND expires_at > CURRENT_TIMESTAMP";
        List<WeatherSnapshot> results = jdbcTemplate.query(sql, ROW_MAPPER, locationHash);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void save(String locationHash, WeatherSnapshot snapshot, int ttlMinutes) {
        String sql = "INSERT INTO weather_cache (location_hash, latitude, longitude, temperature, humidity, rain_probability, wind_speed, weather_code, logistics_context, retrieved_at, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE temperature = VALUES(temperature), humidity = VALUES(humidity), " +
                     "rain_probability = VALUES(rain_probability), wind_speed = VALUES(wind_speed), " +
                     "weather_code = VALUES(weather_code), logistics_context = VALUES(logistics_context), " +
                     "retrieved_at = VALUES(retrieved_at), expires_at = VALUES(expires_at)";
                     
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        LocalDateTime retrievedAt = snapshot.getRetrievedAt() != null ? snapshot.getRetrievedAt() : LocalDateTime.now();

        jdbcTemplate.update(sql, locationHash, snapshot.getLatitude(), snapshot.getLongitude(),
                snapshot.getTemperature(), snapshot.getHumidity(), snapshot.getRainProbability(),
                snapshot.getWindSpeed(), snapshot.getWeatherCode(), snapshot.getLogisticsContext(),
                Timestamp.valueOf(retrievedAt), Timestamp.valueOf(expiresAt));
    }
}
