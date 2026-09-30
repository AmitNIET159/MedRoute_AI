package com.medroute.service;

import com.medroute.dao.EmergencyRequestDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.dao.MedicineDAO;
import com.medroute.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmergencyRequestService.
 * Tests validation, creation, and cancellation logic.
 */
public class EmergencyRequestServiceTest {

    @Mock private EmergencyRequestDAO emergencyRequestDAO;
    @Mock private FacilityDAO facilityDAO;
    @Mock private MedicineDAO medicineDAO;
    @Mock private AuditService auditService;
    @Mock private HttpServletRequest httpRequest;

    private EmergencyRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmergencyRequestService(emergencyRequestDAO, facilityDAO, medicineDAO, auditService);
    }

    @Test
    void testCreateRequest_success() {
        Facility facility = new Facility();
        facility.setId(1L);
        facility.setName("Test Hospital");
        facility.setActive(true);
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(facility));

        Medicine medicine = new Medicine();
        medicine.setId(4L);
        medicine.setName("Amoxicillin");
        when(medicineDAO.findById(4L)).thenReturn(Optional.of(medicine));

        when(emergencyRequestDAO.create(any(EmergencyRequest.class))).thenReturn(99L);

        EmergencyRequest request = new EmergencyRequest();
        request.setMedicineId(4L);
        request.setQuantityNeeded(500);
        request.setUrgency(Urgency.HIGH);
        request.setRequiredByDate(LocalDate.now().plusDays(5));
        request.setReason("Test reason");

        EmergencyRequest result = service.createRequest(request, 2L, 1L, httpRequest);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(RequestStatus.OPEN, result.getStatus());
        assertEquals(0, result.getQuantityFulfilled());
        assertEquals(1L, result.getRequestingFacilityId());
        assertEquals(2L, result.getCreatedBy());

        verify(emergencyRequestDAO).create(any(EmergencyRequest.class));
        verify(auditService).logAction(eq(2L), eq("CREATE_EMERGENCY_REQUEST"),
                eq("EmergencyRequest"), eq("99"), any(HttpServletRequest.class));
    }

    @Test
    void testCreateRequest_invalidQuantity_zero() {
        Facility facility = new Facility();
        facility.setId(1L);
        facility.setActive(true);
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(facility));
        when(medicineDAO.findById(4L)).thenReturn(Optional.of(new Medicine()));

        EmergencyRequest request = new EmergencyRequest();
        request.setMedicineId(4L);
        request.setQuantityNeeded(0);
        request.setUrgency(Urgency.HIGH);
        request.setRequiredByDate(LocalDate.now().plusDays(5));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request, 2L, 1L, httpRequest),
                "Zero quantity should be rejected");

        verify(emergencyRequestDAO, never()).create(any());
    }

    @Test
    void testCreateRequest_invalidQuantity_negative() {
        Facility facility = new Facility();
        facility.setId(1L);
        facility.setActive(true);
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(facility));
        when(medicineDAO.findById(4L)).thenReturn(Optional.of(new Medicine()));

        EmergencyRequest request = new EmergencyRequest();
        request.setMedicineId(4L);
        request.setQuantityNeeded(-10);
        request.setUrgency(Urgency.HIGH);
        request.setRequiredByDate(LocalDate.now().plusDays(5));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request, 2L, 1L, httpRequest),
                "Negative quantity should be rejected");
    }

    @Test
    void testCreateRequest_pastDate_rejected() {
        Facility facility = new Facility();
        facility.setId(1L);
        facility.setActive(true);
        when(facilityDAO.findById(1L)).thenReturn(Optional.of(facility));
        when(medicineDAO.findById(4L)).thenReturn(Optional.of(new Medicine()));

        EmergencyRequest request = new EmergencyRequest();
        request.setMedicineId(4L);
        request.setQuantityNeeded(100);
        request.setUrgency(Urgency.LOW);
        request.setRequiredByDate(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request, 2L, 1L, httpRequest),
                "Past required-by date should be rejected");
    }

    @Test
    void testCreateRequest_facilityNotFound() {
        when(facilityDAO.findById(999L)).thenReturn(Optional.empty());

        EmergencyRequest request = new EmergencyRequest();
        request.setMedicineId(4L);
        request.setQuantityNeeded(100);
        request.setUrgency(Urgency.LOW);
        request.setRequiredByDate(LocalDate.now().plusDays(5));

        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest(request, 2L, 999L, httpRequest));
    }

    @Test
    void testCancelRequest_openStatus_success() {
        EmergencyRequest existing = new EmergencyRequest();
        existing.setId(1L);
        existing.setRequestingFacilityId(1L);
        existing.setStatus(RequestStatus.OPEN);
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(existing));

        assertDoesNotThrow(
                () -> service.cancelRequest(1L, 2L, 1L, false, httpRequest));

        verify(emergencyRequestDAO).updateStatus(1L, RequestStatus.CANCELLED);
    }

    @Test
    void testCancelRequest_fulfilledStatus_rejected() {
        EmergencyRequest existing = new EmergencyRequest();
        existing.setId(1L);
        existing.setRequestingFacilityId(1L);
        existing.setStatus(RequestStatus.FULFILLED);
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> service.cancelRequest(1L, 2L, 1L, false, httpRequest),
                "Cannot cancel FULFILLED request");

        verify(emergencyRequestDAO, never()).updateStatus(anyLong(), any());
    }

    @Test
    void testCancelRequest_wrongFacility_rejected() {
        EmergencyRequest existing = new EmergencyRequest();
        existing.setId(1L);
        existing.setRequestingFacilityId(1L);
        existing.setStatus(RequestStatus.OPEN);
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(existing));

        // User facility 99 is not the owner (1) and not admin
        assertThrows(SecurityException.class,
                () -> service.cancelRequest(1L, 5L, 99L, false, httpRequest),
                "Non-owner non-admin should be rejected");

        verify(emergencyRequestDAO, never()).updateStatus(anyLong(), any());
    }

    @Test
    void testCancelRequest_adminCanCancelAny() {
        EmergencyRequest existing = new EmergencyRequest();
        existing.setId(1L);
        existing.setRequestingFacilityId(1L);
        existing.setStatus(RequestStatus.OPEN);
        when(emergencyRequestDAO.findById(1L)).thenReturn(Optional.of(existing));

        // Admin from facility 99 can still cancel
        assertDoesNotThrow(
                () -> service.cancelRequest(1L, 1L, 99L, true, httpRequest));

        verify(emergencyRequestDAO).updateStatus(1L, RequestStatus.CANCELLED);
    }
}
