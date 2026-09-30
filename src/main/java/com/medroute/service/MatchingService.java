package com.medroute.service;

import com.medroute.dao.DemandDAO;
import com.medroute.dao.EmergencyRequestDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Smart healthcare resource matching engine.
 *
 * All scoring is deterministic — no AI dependency.
 * Reuses Phase 3–5 services: DemandService for risk/consumption,
 * DistanceService for Haversine distance, DemandDAO for inventory batches.
 *
 * Match Score Formula (0–100):
 *   distanceScore × 0.25
 * + surplusScore  × 0.30
 * + donorRiskScore × 0.20
 * + expiryScore   × 0.15
 * + reliabilityScore × 0.10
 */
@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);

    // Configurable constants — centralized
    static final double DEFAULT_MATCHING_RADIUS_KM = 50.0;
    static final double MAX_MATCHING_RADIUS_KM = 100.0;
    static final int DONOR_SAFETY_DAYS = 7;
    static final int MIN_TRANSFER_REMAINING_DAYS = 7;

    // Score weights
    static final double WEIGHT_DISTANCE = 0.25;
    static final double WEIGHT_SURPLUS = 0.30;
    static final double WEIGHT_DONOR_RISK = 0.20;
    static final double WEIGHT_EXPIRY = 0.15;
    static final double WEIGHT_RELIABILITY = 0.10;

    private final EmergencyRequestDAO emergencyRequestDAO;
    private final FacilityDAO facilityDAO;
    private final DemandDAO demandDAO;
    private final DemandService demandService;
    private final DistanceService distanceService;

    public MatchingService(EmergencyRequestDAO emergencyRequestDAO,
                           FacilityDAO facilityDAO,
                           DemandDAO demandDAO,
                           DemandService demandService,
                           DistanceService distanceService) {
        this.emergencyRequestDAO = emergencyRequestDAO;
        this.facilityDAO = facilityDAO;
        this.demandDAO = demandDAO;
        this.demandService = demandService;
        this.distanceService = distanceService;
    }

    /**
     * Finds and scores donor candidates for the given emergency request.
     *
     * @param emergencyRequestId the request to match
     * @return sorted list of eligible candidates, best first
     */
    public List<MatchCandidate> findCandidates(Long emergencyRequestId) {
        return findCandidates(emergencyRequestId, DEFAULT_MATCHING_RADIUS_KM);
    }

    /**
     * Finds and scores donor candidates within the specified radius.
     *
     * @param emergencyRequestId the request to match
     * @param radiusKm search radius (capped at MAX_MATCHING_RADIUS_KM)
     * @return sorted list of eligible candidates, best first
     */
    public List<MatchCandidate> findCandidates(Long emergencyRequestId, double radiusKm) {
        // Clamp radius
        radiusKm = Math.min(radiusKm, MAX_MATCHING_RADIUS_KM);

        // 1. Load and validate request
        EmergencyRequest request = emergencyRequestDAO.findById(emergencyRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency request not found: " + emergencyRequestId));

        if (request.getStatus() != RequestStatus.OPEN &&
            request.getStatus() != RequestStatus.PARTIALLY_FULFILLED) {
            logger.warn("Matching attempted on non-open request: id={}, status={}",
                    emergencyRequestId, request.getStatus());
            return List.of();
        }

        int quantityStillNeeded = request.getRemainingQuantity();
        if (quantityStillNeeded <= 0) {
            return List.of();
        }

        // 2. Load requesting facility coordinates
        Facility requestingFacility = facilityDAO.findById(request.getRequestingFacilityId())
                .orElseThrow(() -> new IllegalStateException("Requesting facility not found"));

        if (requestingFacility.getLatitude() == null || requestingFacility.getLongitude() == null) {
            logger.warn("Requesting facility {} has no coordinates — cannot perform distance matching",
                    requestingFacility.getId());
            return List.of();
        }

        double reqLat = requestingFacility.getLatitude().doubleValue();
        double reqLon = requestingFacility.getLongitude().doubleValue();

        // 3. Find all active facilities (excluding requesting facility)
        List<Facility> allFacilities = facilityDAO.findAllActive();
        List<MatchCandidate> candidates = new ArrayList<>();

        final double finalRadius = radiusKm;

        for (Facility donor : allFacilities) {
            // Skip self
            if (donor.getId().equals(request.getRequestingFacilityId())) continue;

            // Skip facilities without coordinates
            if (donor.getLatitude() == null || donor.getLongitude() == null) continue;

            // 4a. Calculate distance
            double distance = distanceService.calculateDistanceKm(
                    reqLat, reqLon,
                    donor.getLatitude().doubleValue(),
                    donor.getLongitude().doubleValue());

            if (distance > finalRadius) continue;

            // 4b. Query active, non-expired inventory for the requested medicine
            List<InventoryBatch> batches = demandDAO.getActiveBatches(donor.getId(), request.getMedicineId());

            LocalDate minExpiryThreshold = LocalDate.now().plusDays(MIN_TRANSFER_REMAINING_DAYS);

            List<InventoryBatch> eligibleBatches = batches.stream()
                    .filter(b -> b.getStatus() == BatchStatus.ACTIVE)
                    .filter(b -> b.getAvailableQuantity() > 0)
                    .filter(b -> b.getExpiryDate() != null &&
                                 !b.getExpiryDate().isBefore(minExpiryThreshold))
                    .collect(Collectors.toList());

            if (eligibleBatches.isEmpty()) continue;

            // 4c. Calculate available and transferable
            int totalQty = eligibleBatches.stream().mapToInt(InventoryBatch::getQuantity).sum();
            int reservedQty = eligibleBatches.stream().mapToInt(InventoryBatch::getReservedQuantity).sum();
            int available = totalQty - reservedQty;

            if (available <= 0) continue;

            // 4d. Calculate donor safety buffer using Phase 4 DemandService
            Double avgConsumption = null;
            try {
                DemandAnalysis analysis = demandService.analyzeMedicine(donor.getId(), request.getMedicineId());
                if (analysis.getMetrics() != null) {
                    avgConsumption = analysis.getMetrics().getAverageDailyConsumption();
                }
            } catch (Exception e) {
                logger.debug("Could not analyze medicine for donor {}: {}", donor.getId(), e.getMessage());
            }

            int donorBuffer = 0;
            if (avgConsumption != null && avgConsumption > 0) {
                donorBuffer = (int) Math.ceil(avgConsumption * DONOR_SAFETY_DAYS);
            }

            int transferable = available - donorBuffer;
            if (transferable <= 0) continue;

            // Cap transferable to what's actually needed
            transferable = Math.min(transferable, quantityStillNeeded);

            // 4e. Get donor risk level
            String donorRiskLevel;
            try {
                DemandAnalysis analysis = demandService.analyzeMedicine(donor.getId(), request.getMedicineId());
                donorRiskLevel = analysis.getRiskScore() != null ? analysis.getRiskScore().getRiskLevel() : "UNKNOWN";
            } catch (Exception e) {
                donorRiskLevel = "UNKNOWN";
            }

            // Skip CRITICAL risk donors
            if ("CRITICAL".equals(donorRiskLevel)) continue;

            // 4f. Calculate nearest expiry days among eligible batches
            int nearestExpiry = eligibleBatches.stream()
                    .mapToInt(b -> (int) ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate()))
                    .min()
                    .orElse(0);

            // 4g. Build candidate and score
            MatchCandidate candidate = new MatchCandidate();
            candidate.setFacilityId(donor.getId());
            candidate.setFacilityName(donor.getName());
            candidate.setFacilityType(donor.getFacilityType());
            candidate.setCity(donor.getCity());
            candidate.setDistanceKm(distance);
            candidate.setTotalQuantity(totalQty);
            candidate.setReservedQuantity(reservedQty);
            candidate.setAvailableQuantity(available);
            candidate.setTransferableQuantity(transferable);
            candidate.setAverageDailyConsumption(avgConsumption);
            candidate.setDaysOfStock(avgConsumption != null && avgConsumption > 0 ?
                    available / avgConsumption : null);
            candidate.setDonorRiskLevel(donorRiskLevel);
            candidate.setNearestExpiryDays(nearestExpiry);
            candidate.setFacilityReliabilityScore(donor.getReliabilityScore());

            // Score components
            int distScore = calculateDistanceScore(distance);
            int surpScore = calculateSurplusScore(transferable, quantityStillNeeded);
            int riskScore = calculateDonorRiskScore(donorRiskLevel);
            int expScore = calculateExpiryScore(nearestExpiry);
            int relScore = calculateReliabilityScore(donor.getReliabilityScore());

            candidate.setDistanceScore(distScore);
            candidate.setSurplusScore(surpScore);
            candidate.setDonorRiskScoreValue(riskScore);
            candidate.setExpiryScore(expScore);
            candidate.setReliabilityScoreValue(relScore);

            // Composite score
            int matchScore = calculateCompositeScore(distScore, surpScore, riskScore, expScore, relScore);
            candidate.setMatchScore(matchScore);
            candidate.setMatchStatus(getMatchStatus(matchScore));

            candidates.add(candidate);
        }

        // 5. Sort: matchScore DESC, distance ASC, transferableQuantity DESC
        candidates.sort(
                Comparator.comparingInt(MatchCandidate::getMatchScore).reversed()
                        .thenComparingDouble(MatchCandidate::getDistanceKm)
                        .thenComparing(Comparator.comparingInt(MatchCandidate::getTransferableQuantity).reversed())
        );

        logger.info("Matching for request {}: found {} eligible candidates out of {} facilities",
                emergencyRequestId, candidates.size(), allFacilities.size());

        return candidates;
    }

    // ---- Deterministic scoring functions ----

    /**
     * Distance score: closer = higher. Returns 0–100.
     */
    static int calculateDistanceScore(double distanceKm) {
        if (distanceKm <= 5.0) return 100;
        if (distanceKm <= 10.0) return 85;
        if (distanceKm <= 20.0) return 70;
        if (distanceKm <= 35.0) return 50;
        if (distanceKm <= 50.0) return 30;
        return 0; // Beyond radius — should have been filtered, but safe fallback
    }

    /**
     * Surplus score: how much of the requested quantity can be fulfilled. Returns 0–100.
     */
    static int calculateSurplusScore(int transferableQuantity, int requestedQuantity) {
        if (requestedQuantity <= 0) return 0;
        double ratio = (double) transferableQuantity / requestedQuantity;
        if (ratio >= 1.0) return 100;
        if (ratio >= 0.75) return 80;
        if (ratio >= 0.50) return 60;
        if (ratio >= 0.25) return 40;
        return 20;
    }

    /**
     * Donor risk score: prefer LOW-risk donors. Returns 0–100.
     */
    static int calculateDonorRiskScore(String riskLevel) {
        if (riskLevel == null) return 50; // Neutral if unknown
        switch (riskLevel) {
            case "LOW": return 100;
            case "MODERATE": return 70;
            case "HIGH": return 30;
            case "CRITICAL": return 0;
            default: return 50;
        }
    }

    /**
     * Expiry suitability score: more remaining shelf life = higher. Returns 0–100.
     */
    static int calculateExpiryScore(int nearestExpiryDays) {
        if (nearestExpiryDays > 60) return 100;
        if (nearestExpiryDays > 30) return 80;
        if (nearestExpiryDays > 14) return 60;
        if (nearestExpiryDays > 7) return 40;
        if (nearestExpiryDays >= 7) return 20;
        return 0; // Should have been filtered
    }

    /**
     * Reliability score: uses existing Facility.reliabilityScore (0.00–1.00). Returns 0–100.
     */
    static int calculateReliabilityScore(BigDecimal reliabilityScore) {
        if (reliabilityScore == null || reliabilityScore.compareTo(BigDecimal.ZERO) == 0) {
            return 50; // Neutral if no history
        }
        return Math.min(100, Math.max(0, (int) (reliabilityScore.doubleValue() * 100)));
    }

    /**
     * Composite match score with fixed weights. Returns 0–100.
     */
    static int calculateCompositeScore(int distance, int surplus, int donorRisk,
                                        int expiry, int reliability) {
        double weighted = distance * WEIGHT_DISTANCE
                        + surplus * WEIGHT_SURPLUS
                        + donorRisk * WEIGHT_DONOR_RISK
                        + expiry * WEIGHT_EXPIRY
                        + reliability * WEIGHT_RELIABILITY;
        return Math.max(0, Math.min(100, (int) Math.round(weighted)));
    }

    /**
     * Classifies a match score into a human-readable status.
     */
    static String getMatchStatus(int matchScore) {
        if (matchScore >= 70) return "RECOMMENDED";
        if (matchScore >= 50) return "SUITABLE";
        if (matchScore >= 30) return "LOW_CONFIDENCE";
        return "NOT_RECOMMENDED";
    }
}
