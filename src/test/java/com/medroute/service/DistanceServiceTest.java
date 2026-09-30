package com.medroute.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DistanceServiceTest {

    private DistanceService distanceService;

    @BeforeEach
    void setUp() {
        distanceService = new DistanceService();
    }

    @Test
    void testCalculateDistanceIdentical() {
        double distance = distanceService.calculateDistanceKm(40.7128, -74.0060, 40.7128, -74.0060);
        assertEquals(0.0, distance, 0.001, "Distance between identical coordinates must be 0");
    }

    @Test
    void testCalculateDistanceKnownPair() {
        // approximate distance from NY to LA is ~3940 km
        // NY: 40.7128 N, 74.0060 W
        // LA: 34.0522 N, 118.2437 W
        double distance = distanceService.calculateDistanceKm(40.7128, -74.0060, 34.0522, -118.2437);
        assertTrue(distance > 3900 && distance < 4000, "Distance NY-LA should be ~3940km, got " + distance);
    }
}
