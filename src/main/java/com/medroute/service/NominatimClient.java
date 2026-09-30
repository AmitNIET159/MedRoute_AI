package com.medroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medroute.model.GeocodingResult;
import com.medroute.util.NominatimRateLimiter;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Service
public class NominatimClient {
    private static final Logger logger = LoggerFactory.getLogger(NominatimClient.class);
    private final String baseUrl;
    private final String userAgent;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public NominatimClient(Dotenv dotenv, ObjectMapper objectMapper, @Autowired(required = false) HttpClient httpClient) {
        this.baseUrl = dotenv.get("NOMINATIM_BASE_URL", "https://nominatim.openstreetmap.org");
        this.userAgent = dotenv.get("NOMINATIM_USER_AGENT", "MedRouteAI/1.0");
        this.objectMapper = objectMapper;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<GeocodingResult> search(String query) {
        try {
            NominatimRateLimiter.acquire();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/search?q=" + encodedQuery + "&format=json&addressdetails=1&limit=1"))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Nominatim API error: {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                GeocodingResult result = new GeocodingResult();
                result.setLatitude(first.path("lat").asDouble());
                result.setLongitude(first.path("lon").asDouble());
                result.setDisplayName(first.path("display_name").asText());
                result.setAddressJson(first.path("address").toString());
                return Optional.of(result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Nominatim request interrupted", e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Exception during Nominatim search", e);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<GeocodingResult> reverse(Double lat, Double lng) {
        try {
            NominatimRateLimiter.acquire();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/reverse?lat=" + lat + "&lon=" + lng + "&format=json&addressdetails=1"))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Nominatim API error: {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.has("error")) {
                GeocodingResult result = new GeocodingResult();
                result.setLatitude(root.path("lat").asDouble(lat)); // use provided if missing
                result.setLongitude(root.path("lon").asDouble(lng));
                result.setDisplayName(root.path("display_name").asText());
                result.setAddressJson(root.path("address").toString());
                return Optional.of(result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Nominatim reverse request interrupted", e);
        } catch (Exception e) {
            logger.error("Exception during Nominatim reverse", e);
        }
        return Optional.empty();
    }
}
