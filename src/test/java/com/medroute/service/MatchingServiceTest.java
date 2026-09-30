package com.medroute.service;

import com.medroute.dao.DemandDAO;
import com.medroute.dao.EmergencyRequestDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the deterministic matching engine.
 * Tests verify scoring formulas, eligibility filters, and sort order.
 */
public class MatchingServiceTest {

    @Mock private EmergencyRequestDAO emergencyRequestDAO;
    @Mock private FacilityDAO facilityDAO;
    @Mock private DemandDAO demandDAO;
    @Mock private DemandService demandService;
    @Mock private DistanceService distanceService;

    private MatchingService matchingService;

    // Reusable test data
    private EmergencyRequest openRequest;
    private Facility requestingFacility;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        matchingService = new MatchingService(
                emergencyRequestDAO, facilityDAO, demandDAO, demandService, distanceService);

        // Default open request: facility 1 needs 1000 units of medicine 4
        openRequest = new EmergencyRequest();
        openRequest.setId(1L);
        openRequest.setRequestingFacilityId(1L);
        openRequest.setMedicineId(4L);
        openRequest.setQuantityNeeded(1000);
        openRequest.setQuantityFulfilled(0);
        openRequest.setStatus(RequestStatus.OPEN);
        openRequest.setUrgency(Urgency.HIGH);
        openRequest.setRequiredByDate(LocalDate.now().plusDays(5));

        // Requesting facility with coordinates (Mumbai)
        requestingFacility = new Facility();
        requestingFacility.setId(1L);
        requestingFacility.setName("City General Hospital");
        requestingFacility.setLatitude(new BigDecimal("19.0760"));
        requestingFacility.setLongitude(new BigDecimal("72.8777"));
        requestingFacility.setActive(true);
    }

    // --- Static scoring function tests ---

    @Test
    void testDistanceScore_veryClose() {
        assertEquals(100, MatchingService.calculateDistanceScore(3.0));
    }

    @Test
    void testDistanceScore_boundaries() {
        assertEquals(100, MatchingService.calculateDistanceScore(5.0));
        assertEquals(85, MatchingService.calculateDistanceScore(8.0));
        assertEquals(70, MatchingService.calculateDistanceScore(15.0));
        assertEquals(50, MatchingService.calculateDistanceScore(25.0));
        assertEquals(30, MatchingService.calculateDistanceScore(45.0));
        assertEquals(0, MatchingService.calculateDistanceScore(60.0));
    }

    @Test
    void testSurplusScore_fullCoverage() {
        assertEquals(100, MatchingService.calculateSurplusScore(1000, 1000));
        assertEquals(100, MatchingService.calculateSurplusScore(1500, 1000));
    }

    @Test
    void testSurplusScore_partialCoverage() {
        assertEquals(80, MatchingService.calculateSurplusScore(800, 1000));
        assertEquals(60, MatchingService.calculateSurplusScore(600, 1000));
        assertEquals(40, MatchingService.calculateSurplusScore(300, 1000));
        assertEquals(20, MatchingService.calculateSurplusScore(100, 1000));
    }

    @Test
    void testSurplusScore_zeroDivision() {
        assertEquals(0, MatchingService.calculateSurplusScore(100, 0));
    }

    @Test
    void testDonorRiskScore_values() {
        assertEquals(100, MatchingService.calculateDonorRiskScore("LOW"));
        assertEquals(70, MatchingService.calculateDonorRiskScore("MODERATE"));
        assertEquals(30, MatchingService.calculateDonorRiskScore("HIGH"));
        assertEquals(0, MatchingService.calculateDonorRiskScore("CRITICAL"));
        assertEquals(50, MatchingService.calculateDonorRiskScore(null));
        assertEquals(50, MatchingService.calculateDonorRiskScore("UNKNOWN"));
    }

    @Test
    void testExpiryScore_values() {
        assertEquals(100, MatchingService.calculateExpiryScore(90));
        assertEquals(80, MatchingService.calculateExpiryScore(45));
        assertEquals(60, MatchingService.calculateExpiryScore(20));
        assertEquals(40, MatchingService.calculateExpiryScore(10));
        assertEquals(20, MatchingService.calculateExpiryScore(7));
        assertEquals(0, MatchingService.calculateExpiryScore(5));
    }

    @Test
    void testReliabilityScore_values() {
        assertEquals(95, MatchingService.calculateReliabilityScore(new BigDecimal("0.95")));
        assertEquals(80, MatchingService.calculateReliabilityScore(new BigDecimal("0.80")));
        assertEquals(50, MatchingService.calculateReliabilityScore(null));
        assertEquals(50, MatchingService.calculateReliabilityScore(BigDecimal.ZERO));
    }

    @Test
    void testCompositeScore_deterministic() {
        // distance=100, surplus=100, risk=100, expiry=100, reliability=100
        assertEquals(100, MatchingService.calculateCompositeScore(100, 100, 100, 100, 100));
        // All zeros
        assertEquals(0, MatchingService.calculateCompositeScore(0, 0, 0, 0, 0));
        // Weighted: 100*0.25 + 80*0.30 + 70*0.20 + 60*0.15 + 50*0.10
        // = 25 + 24 + 14 + 9 + 5 = 77
        assertEquals(77, MatchingService.calculateCompositeScore(100, 80, 70, 60, 50));
    }

    @Test
    void testMatchStatus_classification() {
        assertEquals("RECOMMENDED", MatchingService.getMatchStatus(70));
        assertEquals("RECOMMENDED", MatchingService.getMatchStatus(95));
        assertEquals("SUITABLE", MatchingService.getMatchStatus(50));
        assertEquals("SUITABLE", MatchingService.getMatchStatus(69));
        assertEquals("LOW_CONFIDENCE", MatchingService.getMatchStatus(30));
        assertEquals("LOW_CONFIDENCE", MatchingService.getMatchStatus(49));
        assertEquals("NOT_RECOMMENDED", MatchingService.getMatchStatus(29));
        assertEquals("NOT_RECOMMENDED", MatchingService.getMatchStatus(0));
    }

    // --- Integration-level tests (mocked DAOs) ---

    @Test
    void testFindCandidates_noCandidatesWhenNoOtherFacilities() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));
        // Only the requesting facility exists
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility));

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "No candidates when no other facilities exist");
    }

    @Test
    void testFindCandidates_donorBeyondRadius_excluded() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility farDonor = createDonorFacility(2L, "Far Hospital", "28.5672", "77.2100", "0.92");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, farDonor));
        // Distance > 50 km (default radius)
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1200.0);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "Donor beyond radius should be excluded");
    }

    @Test
    void testFindCandidates_donorInsufficientStock_excluded() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Donor Hospital", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);
        // No batches for this medicine
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of());

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "Donor with no stock should be excluded");
    }

    @Test
    void testFindCandidates_expiredBatchesExcluded() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Donor Hospital", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);

        // Batch expires in 3 days (< MIN_TRANSFER_REMAINING_DAYS=7)
        InventoryBatch expiring = createBatch(10L, 2L, 4L, 500, 0,
                LocalDate.now().plusDays(3), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(expiring));

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "Batches expiring within MIN_TRANSFER_REMAINING_DAYS should be excluded");
    }

    @Test
    void testFindCandidates_criticalRiskDonor_excluded() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Critical Risk Donor", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);

        InventoryBatch batch = createBatch(10L, 2L, 4L, 2000, 0,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(batch));

        // DemandService returns CRITICAL risk
        DemandAnalysis criticalAnalysis = createAnalysis(null, "CRITICAL");
        when(demandService.analyzeMedicine(2L, 4L)).thenReturn(criticalAnalysis);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "CRITICAL risk donors should be excluded");
    }

    @Test
    void testFindCandidates_safetyBuffer_reducesTransferable() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Buffer Test Donor", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);

        // Donor has 500 units, consumption=50/day, safety=7 days → buffer=350, transferable=150
        InventoryBatch batch = createBatch(10L, 2L, 4L, 500, 0,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(batch));

        DemandAnalysis analysis = createAnalysis(50.0, "LOW");
        when(demandService.analyzeMedicine(2L, 4L)).thenReturn(analysis);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertEquals(1, result.size());
        // transferable = 500 - ceil(50*7) = 500 - 350 = 150, but capped to 150 (< 1000 needed)
        assertEquals(150, result.get(0).getTransferableQuantity());
    }

    @Test
    void testFindCandidates_safetyBuffer_excludesDonorWithInsufficientSurplus() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Tight Stock Donor", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);

        // Donor has 350 units, consumption=50/day → buffer=350 → transferable=0
        InventoryBatch batch = createBatch(10L, 2L, 4L, 350, 0,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(batch));

        DemandAnalysis analysis = createAnalysis(50.0, "LOW");
        when(demandService.analyzeMedicine(2L, 4L)).thenReturn(analysis);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertTrue(result.isEmpty(), "Donor with transferable <= 0 after safety buffer should be excluded");
    }

    @Test
    void testFindCandidates_sortOrder_scoreDescDistanceAsc() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility near = createDonorFacility(2L, "Near Low-Stock", "19.08", "72.88", "0.60");
        Facility far = createDonorFacility(3L, "Far Full-Stock", "19.10", "72.90", "0.95");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, near, far));

        // Near = 5km, Far = 30km
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), eq(19.08), eq(72.88)))
                .thenReturn(5.0);
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), eq(19.10), eq(72.90)))
                .thenReturn(30.0);

        // Near has 200 transferable, Far has 2000 transferable
        InventoryBatch nearBatch = createBatch(10L, 2L, 4L, 200, 0,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        InventoryBatch farBatch = createBatch(11L, 3L, 4L, 2000, 0,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(nearBatch));
        when(demandDAO.getActiveBatches(3L, 4L)).thenReturn(List.of(farBatch));

        DemandAnalysis lowAnalysis = createAnalysis(null, "LOW");
        when(demandService.analyzeMedicine(anyLong(), eq(4L))).thenReturn(lowAnalysis);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertEquals(2, result.size());
        // Verify sorted by score DESC (higher score first)
        assertTrue(result.get(0).getMatchScore() >= result.get(1).getMatchScore(),
                "Results should be sorted by match score descending");
    }

    @Test
    void testFindCandidates_nonOpenRequest_returnsEmpty() {
        EmergencyRequest fulfilledRequest = new EmergencyRequest();
        fulfilledRequest.setId(99L);
        fulfilledRequest.setStatus(RequestStatus.FULFILLED);
        when(emergencyRequestDAO.findById(99L)).thenReturn(Optional.of(fulfilledRequest));

        List<MatchCandidate> result = matchingService.findCandidates(99L);
        assertTrue(result.isEmpty(), "Non-open request should return empty candidates");
    }

    @Test
    void testFindCandidates_reservedQuantityReducesAvailable() {
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(openRequest));
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(requestingFacility));

        Facility donor = createDonorFacility(2L, "Reserved Stock Donor", "19.08", "72.88", "0.90");
        when(facilityDAO.findAllActive()).thenReturn(List.of(requestingFacility, donor));
        when(distanceService.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5.0);

        // 1000 total, 800 reserved → 200 available → transferable = 200 (no consumption data)
        InventoryBatch batch = createBatch(10L, 2L, 4L, 1000, 800,
                LocalDate.now().plusDays(90), BatchStatus.ACTIVE);
        when(demandDAO.getActiveBatches(2L, 4L)).thenReturn(List.of(batch));

        DemandAnalysis analysis = createAnalysis(null, "LOW");
        when(demandService.analyzeMedicine(2L, 4L)).thenReturn(analysis);

        List<MatchCandidate> result = matchingService.findCandidates(1L);
        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getAvailableQuantity());
        assertEquals(200, result.get(0).getTransferableQuantity()); // No consumption data → no buffer
    }

    // --- Helper methods ---

    private Facility createDonorFacility(Long id, String name, String lat, String lon, String reliability) {
        Facility f = new Facility();
        f.setId(id);
        f.setName(name);
        f.setFacilityType(FacilityType.HOSPITAL);
        f.setCity("TestCity");
        f.setLatitude(new BigDecimal(lat));
        f.setLongitude(new BigDecimal(lon));
        f.setReliabilityScore(new BigDecimal(reliability));
        f.setActive(true);
        return f;
    }

    private InventoryBatch createBatch(Long id, Long facilityId, Long medicineId,
                                        int quantity, int reserved, LocalDate expiry, BatchStatus status) {
        InventoryBatch b = new InventoryBatch();
        b.setId(id);
        b.setFacilityId(facilityId);
        b.setMedicineId(medicineId);
        b.setQuantity(quantity);
        b.setReservedQuantity(reserved);
        b.setExpiryDate(expiry);
        b.setStatus(status);
        return b;
    }

    private DemandAnalysis createAnalysis(Double avgConsumption, String riskLevel) {
        DemandMetrics metrics = new DemandMetrics();
        metrics.setAverageDailyConsumption(avgConsumption);

        RiskScore risk = new RiskScore();
        risk.setRiskLevel(riskLevel);

        DemandAnalysis analysis = new DemandAnalysis();
        analysis.setMetrics(metrics);
        analysis.setRiskScore(risk);
        return analysis;
    }
}
