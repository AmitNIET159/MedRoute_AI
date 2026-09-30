package com.medroute.service;

import com.medroute.dao.FacilityDAO;
import com.medroute.dao.GeocodingCacheDAO;
import com.medroute.model.Facility;
import com.medroute.model.FacilityLocationDTO;
import com.medroute.model.GeocodingResult;
import com.medroute.model.WeatherSnapshot;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LocationService {
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    private final GeocodingCacheDAO geocodingCacheDAO;
    private final NominatimClient nominatimClient;
    private final FacilityDAO facilityDAO;
    private final DistanceService distanceService;
    private final DemandService demandService;
    private final WeatherService weatherService;
    private final int cacheTtlDays;

    public LocationService(GeocodingCacheDAO geocodingCacheDAO, NominatimClient nominatimClient,
                           FacilityDAO facilityDAO, DistanceService distanceService,
                           DemandService demandService, WeatherService weatherService,
                           Dotenv dotenv) {
        this.geocodingCacheDAO = geocodingCacheDAO;
        this.nominatimClient = nominatimClient;
        this.facilityDAO = facilityDAO;
        this.distanceService = distanceService;
        this.demandService = demandService;
        this.weatherService = weatherService;
        
        String ttl = dotenv.get("GEOCODING_CACHE_TTL_DAYS");
        this.cacheTtlDays = (ttl != null && !ttl.trim().isEmpty()) ? Integer.parseInt(ttl) : 30;
    }

    /**
     * Geocodes an address, falling back to cache if available.
     */
    public Optional<GeocodingResult> geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedAddress = address.trim().toLowerCase();
        String queryHash = generateHash(normalizedAddress);

        try {
            Optional<GeocodingResult> cached = geocodingCacheDAO.findByQueryHash(queryHash);
            if (cached.isPresent()) {
                logger.debug("Geocoding cache hit for query: {}", normalizedAddress);
                return cached;
            }

            logger.info("Geocoding cache miss, querying Nominatim for: {}", normalizedAddress);
            Optional<GeocodingResult> resultOpt = nominatimClient.search(normalizedAddress);
            if (resultOpt.isPresent() && resultOpt.get().getLatitude() != null && resultOpt.get().getLongitude() != null) {
                geocodingCacheDAO.save(queryHash, normalizedAddress, resultOpt.get(), cacheTtlDays);
                return resultOpt;
            }
        } catch (Exception e) {
            logger.error("Geocoding failed for address: {}", normalizedAddress, e);
        }

        return Optional.empty();
    }

    /**
     * Discovers nearby facilities.
     * Integrates Phase 4 DemandService for risk context but prevents duplicate risk calculations per HuggingFace.
     */
    public List<FacilityLocationDTO> findNearbyFacilities(Long sourceFacilityId, double radiusKm) {
        // Enforce bounds
        if (radiusKm < 1.0) radiusKm = 1.0;
        if (radiusKm > 200.0) radiusKm = 200.0; // max logical limit

        Optional<Facility> optSource = facilityDAO.findById(sourceFacilityId);
        if (optSource.isEmpty() || optSource.get().getLatitude() == null) {
            return new ArrayList<>(); // Cannot discover without source coordinates
        }

        Facility sourceFacility = optSource.get();
        double srcLat = sourceFacility.getLatitude().doubleValue();
        double srcLon = sourceFacility.getLongitude().doubleValue();

        // Get active bounded limit to prevent fetching huge lists
        List<Facility> allActive = facilityDAO.findActiveBounded(200);
        List<FacilityLocationDTO> nearby = new ArrayList<>();

        for (Facility f : allActive) {
            if (f.getId().equals(sourceFacilityId) || f.getLatitude() == null) continue;

            double dist = distanceService.calculateDistanceKm(srcLat, srcLon, f.getLatitude().doubleValue(), f.getLongitude().doubleValue());
            if (dist <= radiusKm) {
                FacilityLocationDTO dto = new FacilityLocationDTO();
                dto.setFacility(f);
                dto.setDistanceKm(dist);

                // Fetch Phase 4 context safely
                try {
                    // Only use deterministic DemandService risk score, DO NOT call AI here.
                    String riskScore = demandService.calculateFacilityRiskScore(f.getId()); // Helper to be added/reused
                    dto.setCurrentRisk(riskScore);
                } catch (Exception e) {
                    dto.setCurrentRisk("UNKNOWN");
                }
                
                nearby.add(dto);
            }
        }

        nearby.sort((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));
        return nearby;
    }

    public List<FacilityLocationDTO> findAllBoundedFacilities(int limit) {
        List<Facility> allActive = facilityDAO.findActiveBounded(limit);
        List<FacilityLocationDTO> dtos = new ArrayList<>();
        for (Facility f : allActive) {
            if (f.getLatitude() == null) continue;
            FacilityLocationDTO dto = new FacilityLocationDTO();
            dto.setFacility(f);
            try {
                String riskScore = demandService.calculateFacilityRiskScore(f.getId());
                dto.setCurrentRisk(riskScore);
            } catch (Exception e) {
                dto.setCurrentRisk("UNKNOWN");
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public boolean updateFacilityCoordinates(Long facilityId, Double latitude, Double longitude) {
        // Validation -90 to +90, -180 to +180
        if (latitude == null || latitude < -90.0 || latitude > 90.0) return false;
        if (longitude == null || longitude < -180.0 || longitude > 180.0) return false;

        facilityDAO.updateCoordinates(facilityId, latitude, longitude);
        return true;
    }

    private String generateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
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
            throw new RuntimeException("SHA-256 not found", e);
        }
    }
}
