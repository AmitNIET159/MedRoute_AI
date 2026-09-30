package com.medroute.service;

import com.medroute.dao.FacilityDAO;
import com.medroute.dao.GeocodingCacheDAO;
import com.medroute.model.Facility;
import com.medroute.model.FacilityLocationDTO;
import com.medroute.model.GeocodingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LocationServiceTest {

    private LocationService locationService;
    private GeocodingCacheDAO mockGeocodingCacheDAO;
    private FacilityDAO mockFacilityDAO;
    private NominatimClient mockNominatimClient;
    private DistanceService mockDistanceService;
    private DemandService mockDemandService;

    @BeforeEach
    void setUp() {
        mockGeocodingCacheDAO = Mockito.mock(GeocodingCacheDAO.class);
        mockFacilityDAO = Mockito.mock(FacilityDAO.class);
        mockNominatimClient = Mockito.mock(NominatimClient.class);
        mockDistanceService = Mockito.mock(DistanceService.class);
        mockDemandService = Mockito.mock(DemandService.class);
        WeatherService mockWeatherService = Mockito.mock(WeatherService.class);
        io.github.cdimascio.dotenv.Dotenv mockDotenv = Mockito.mock(io.github.cdimascio.dotenv.Dotenv.class);
        when(mockDotenv.get("GEOCODING_CACHE_TTL_DAYS")).thenReturn("30");

        locationService = new LocationService(
                mockGeocodingCacheDAO,
                mockNominatimClient,
                mockFacilityDAO,
                mockDistanceService,
                mockDemandService,
                mockWeatherService,
                mockDotenv
        );
    }

    @Test
    void testGeocodeAddressCacheHit() {
        GeocodingResult mockResult = new GeocodingResult();
        mockResult.setLatitude(40.71);
        mockResult.setLongitude(-74.01);
        when(mockGeocodingCacheDAO.findByQueryHash(anyString())).thenReturn(Optional.of(mockResult));

        Optional<GeocodingResult> result = locationService.geocodeAddress("New York");
        assertTrue(result.isPresent());
        assertEquals(40.71, result.get().getLatitude());
        verify(mockNominatimClient, never()).search(anyString());
    }

    @Test
    void testGeocodeAddressCacheMiss() {
        when(mockGeocodingCacheDAO.findByQueryHash(anyString())).thenReturn(Optional.empty());
        GeocodingResult mockResult = new GeocodingResult();
        mockResult.setLatitude(40.71);
        mockResult.setLongitude(-74.01);
        when(mockNominatimClient.search(anyString())).thenReturn(Optional.of(mockResult));

        Optional<GeocodingResult> result = locationService.geocodeAddress("New York");
        assertTrue(result.isPresent());
        verify(mockNominatimClient, times(1)).search(anyString());
        verify(mockGeocodingCacheDAO, times(1)).save(anyString(), anyString(), any(GeocodingResult.class), anyInt());
    }

    @Test
    void testFindNearbyFacilities() {
        Facility source = new Facility();
        source.setId(1L);
        source.setLatitude(BigDecimal.valueOf(40.71));
        source.setLongitude(BigDecimal.valueOf(-74.01));
        when(mockFacilityDAO.findById(1L)).thenReturn(Optional.of(source));

        Facility nearbyFac = new Facility();
        nearbyFac.setId(2L);
        nearbyFac.setLatitude(BigDecimal.valueOf(40.72));
        nearbyFac.setLongitude(BigDecimal.valueOf(-74.02));

        when(mockFacilityDAO.findActiveBounded(200)).thenReturn(Arrays.asList(source, nearbyFac));
        when(mockDistanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(5.0);
        when(mockDemandService.calculateFacilityRiskScore(2L)).thenReturn("HIGH");

        List<FacilityLocationDTO> nearby = locationService.findNearbyFacilities(1L, 10.0);
        assertEquals(1, nearby.size());
        assertEquals(2L, nearby.get(0).getFacility().getId());
        assertEquals(5.0, nearby.get(0).getDistanceKm());
        assertEquals("HIGH", nearby.get(0).getCurrentRisk());
    }
}
