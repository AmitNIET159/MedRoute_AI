package com.medroute.service;

import com.medroute.dao.WeatherCacheDAO;
import com.medroute.model.WeatherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WeatherServiceTest {

    private WeatherService weatherService;
    private WeatherCacheDAO mockWeatherCacheDAO;
    private OpenMeteoClient mockOpenMeteoClient;

    @BeforeEach
    void setUp() {
        mockWeatherCacheDAO = Mockito.mock(WeatherCacheDAO.class);
        mockOpenMeteoClient = Mockito.mock(OpenMeteoClient.class);
        io.github.cdimascio.dotenv.Dotenv mockDotenv = Mockito.mock(io.github.cdimascio.dotenv.Dotenv.class);
        when(mockDotenv.get("WEATHER_CACHE_TTL_MINUTES")).thenReturn("60");
        
        weatherService = new WeatherService(mockWeatherCacheDAO, mockOpenMeteoClient, mockDotenv);
    }

    @Test
    void testGetWeatherContextCacheHit() {
        WeatherSnapshot mockSnapshot = new WeatherSnapshot();
        mockSnapshot.setTemperature(20.0);
        when(mockWeatherCacheDAO.findByLocationHash(anyString())).thenReturn(Optional.of(mockSnapshot));

        Optional<WeatherSnapshot> result = weatherService.getWeatherContext(40.71, -74.01);
        
        assertTrue(result.isPresent());
        assertEquals(20.0, result.get().getTemperature());
        verify(mockOpenMeteoClient, never()).getWeather(anyDouble(), anyDouble());
    }

    @Test
    void testGetWeatherContextCacheMiss() {
        when(mockWeatherCacheDAO.findByLocationHash(anyString())).thenReturn(Optional.empty());
        
        WeatherSnapshot mockSnapshot = new WeatherSnapshot();
        mockSnapshot.setTemperature(25.0);
        when(mockOpenMeteoClient.getWeather(anyDouble(), anyDouble())).thenReturn(Optional.of(mockSnapshot));

        Optional<WeatherSnapshot> result = weatherService.getWeatherContext(40.71, -74.01);
        
        assertTrue(result.isPresent());
        assertEquals(25.0, result.get().getTemperature());
        verify(mockOpenMeteoClient, times(1)).getWeather(anyDouble(), anyDouble());
        verify(mockWeatherCacheDAO, times(1)).save(anyString(), any(WeatherSnapshot.class), anyInt());
    }

    @Test
    void testGetWeatherContextUnavailable() {
        when(mockWeatherCacheDAO.findByLocationHash(anyString())).thenReturn(Optional.empty());
        when(mockOpenMeteoClient.getWeather(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        Optional<WeatherSnapshot> result = weatherService.getWeatherContext(40.71, -74.01);
        
        assertFalse(result.isPresent());
        verify(mockOpenMeteoClient, times(1)).getWeather(anyDouble(), anyDouble());
        verify(mockWeatherCacheDAO, never()).save(anyString(), any(), anyInt());
    }
}
