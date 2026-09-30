package com.medroute.service;

import com.medroute.model.GeocodingResult;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NominatimClientTest {

    private NominatimClient nominatimClient;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        Dotenv mockDotenv = Mockito.mock(Dotenv.class);
        when(mockDotenv.get(Mockito.eq("NOMINATIM_BASE_URL"), Mockito.anyString())).thenReturn("https://nominatim.openstreetmap.org");
        when(mockDotenv.get(Mockito.eq("NOMINATIM_USER_AGENT"), Mockito.anyString())).thenReturn("MockAgent/1.0");

        mockHttpClient = Mockito.mock(HttpClient.class);
        com.fasterxml.jackson.databind.ObjectMapper realObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        // Use subclass mock maker to bypass JDK 26 issues (handled by pom.xml)
        nominatimClient = new NominatimClient(mockDotenv, realObjectMapper, mockHttpClient);
    }

    @Test
    void testSearchSuccess() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        String jsonResponse = "[{\"lat\":\"40.7128\", \"lon\":\"-74.0060\", \"display_name\":\"New York, NY\"}]";
        when(mockResponse.body()).thenReturn(jsonResponse);

        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<GeocodingResult> resultOpt = nominatimClient.search("New York");
        assertTrue(resultOpt.isPresent());
        assertEquals(40.7128, resultOpt.get().getLatitude());
        assertEquals(-74.0060, resultOpt.get().getLongitude());
        assertEquals("New York, NY", resultOpt.get().getDisplayName());
    }

    @Test
    void testSearchEmptyResult() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("[]");

        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<GeocodingResult> resultOpt = nominatimClient.search("InvalidPlaceXYZ123");
        assertFalse(resultOpt.isPresent());
    }

    @Test
    void testRateLimitOrError() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(429); // Too Many Requests
        
        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<GeocodingResult> resultOpt = nominatimClient.search("New York");
        assertFalse(resultOpt.isPresent());
    }
}
