package com.medroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medroute.model.WeatherSnapshot;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OpenMeteoClient {
    private static final Logger logger = LoggerFactory.getLogger(OpenMeteoClient.class);
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OpenMeteoClient(Dotenv dotenv, ObjectMapper objectMapper, @Autowired(required = false) HttpClient httpClient) {
        this.baseUrl = dotenv.get("OPEN_METEO_BASE_URL", "https://api.open-meteo.com/v1/forecast");
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<WeatherSnapshot> getWeather(Double lat, Double lng) {
        try {
            String url = String.format("%s?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m",
                    baseUrl, lat, lng);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("OpenMeteo API error: {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.path("current");
            if (!current.isMissingNode()) {
                WeatherSnapshot snapshot = new WeatherSnapshot();
                snapshot.setLatitude(lat);
                snapshot.setLongitude(lng);
                snapshot.setTemperature(current.path("temperature_2m").asDouble());
                snapshot.setHumidity(current.path("relative_humidity_2m").asDouble());
                snapshot.setRainProbability(current.path("precipitation_probability").asDouble());
                snapshot.setWindSpeed(current.path("wind_speed_10m").asDouble());
                snapshot.setWeatherCode(current.path("weather_code").asInt());
                snapshot.setRetrievedAt(LocalDateTime.now());
                snapshot.setLogisticsContext(determineContext(snapshot.getWeatherCode()));
                return Optional.of(snapshot);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Open-Meteo request interrupted", e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Exception during Open-Meteo call", e);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private String determineContext(int weatherCode) {
        // Based on WMO Weather interpretation codes
        if (weatherCode == 0) return "Clear conditions";
        if (weatherCode >= 1 && weatherCode <= 3) return "Partly cloudy";
        if (weatherCode >= 51 && weatherCode <= 67) return "Rain";
        if (weatherCode >= 71 && weatherCode <= 77) return "Snow";
        if (weatherCode >= 95) return "Thunderstorm";
        return "Normal conditions";
    }
}
