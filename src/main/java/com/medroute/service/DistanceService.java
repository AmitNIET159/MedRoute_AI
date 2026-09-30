package com.medroute.service;

import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates deterministic geographic distance using Haversine formula.
     * Returns 0.0 if both coordinates are exactly the same.
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
