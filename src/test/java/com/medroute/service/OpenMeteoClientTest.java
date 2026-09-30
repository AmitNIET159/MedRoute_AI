package com.medroute.service;

import com.medroute.model.WeatherSnapshot;
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

public class OpenMeteoClientTest {

    private OpenMeteoClient openMeteoClient;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        Dotenv mockDotenv = Mockito.mock(Dotenv.class);
        when(mockDotenv.get(Mockito.eq("OPEN_METEO_BASE_URL"), Mockito.anyString())).thenReturn("https://api.open-meteo.com/v1/forecast");

        mockHttpClient = Mockito.mock(HttpClient.class);
        com.fasterxml.jackson.databind.ObjectMapper realObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        openMeteoClient = new OpenMeteoClient(mockDotenv, realObjectMapper, mockHttpClient);
    }

    @Test
    void testGetWeatherSuccess() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        
        String jsonResponse = "{\"latitude\": 40.71, \"longitude\": -74.01, \"current\": {\"temperature_2m\": 22.5, \"weather_code\": 3, \"wind_speed_10m\": 15.0, \"precipitation_probability\": 10.0}}";
        when(mockResponse.body()).thenReturn(jsonResponse);

        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<WeatherSnapshot> snapshotOpt = openMeteoClient.getWeather(40.7128, -74.0060);
        assertTrue(snapshotOpt.isPresent());
        assertEquals(22.5, snapshotOpt.get().getTemperature());
        assertEquals(3, snapshotOpt.get().getWeatherCode());
    }

    @Test
    void testGetWeatherMalformedJson() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{ malformed json ]");

        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<WeatherSnapshot> snapshotOpt = openMeteoClient.getWeather(40.71, -74.01);
        assertFalse(snapshotOpt.isPresent());
    }

    @Test
    void testGetWeatherHttp429() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(429);
        
        when(mockHttpClient.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        Optional<WeatherSnapshot> snapshotOpt = openMeteoClient.getWeather(40.71, -74.01);
        assertFalse(snapshotOpt.isPresent());
    }
}
