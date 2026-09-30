package com.medroute.service;

import com.medroute.dao.WeatherCacheDAO;
import com.medroute.model.WeatherSnapshot;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherCacheDAO weatherCacheDAO;
    private final OpenMeteoClient openMeteoClient;
    private final int cacheTtlMinutes;

    public WeatherService(WeatherCacheDAO weatherCacheDAO, OpenMeteoClient openMeteoClient, Dotenv dotenv) {
        this.weatherCacheDAO = weatherCacheDAO;
        this.openMeteoClient = openMeteoClient;
        String ttl = dotenv.get("WEATHER_CACHE_TTL_MINUTES");
        this.cacheTtlMinutes = (ttl != null && !ttl.trim().isEmpty()) ? Integer.parseInt(ttl) : 60;
    }

    /**
     * Gets environmental logistics context weather snapshot.
     */
    public Optional<WeatherSnapshot> getWeatherContext(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }

        // Normalize coordinate cache key (e.g., 2 decimal places to group nearby)
        String locationHash = generateLocationHash(lat, lng);

        try {
            Optional<WeatherSnapshot> cached = weatherCacheDAO.findByLocationHash(locationHash);
            if (cached.isPresent()) {
                logger.debug("Weather cache hit for hash {}", locationHash);
                return cached;
            }

            logger.info("Weather cache miss for hash {}, calling Open-Meteo", locationHash);
            Optional<WeatherSnapshot> snapshotOpt = openMeteoClient.getWeather(lat, lng);
            if (snapshotOpt.isPresent()) {
                weatherCacheDAO.save(locationHash, snapshotOpt.get(), cacheTtlMinutes);
                return snapshotOpt;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve weather context", e);
        }

        return Optional.empty();
    }

    private String generateLocationHash(Double lat, Double lng) {
        // Round to 3 decimal places for caching (~110m precision)
        String key = String.format("%.3f,%.3f", lat, lng);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
