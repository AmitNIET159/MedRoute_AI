package com.medroute.service;

import com.medroute.dao.EmergencyRequestDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.dao.MedicineDAO;
import com.medroute.model.EmergencyRequest;
import com.medroute.model.Facility;
import com.medroute.model.Medicine;
import com.medroute.model.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EmergencyRequestService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyRequestService.class);

    private final EmergencyRequestDAO emergencyRequestDAO;
    private final FacilityDAO facilityDAO;
    private final MedicineDAO medicineDAO;
    private final AuditService auditService;

    public EmergencyRequestService(EmergencyRequestDAO emergencyRequestDAO,
                                   FacilityDAO facilityDAO,
                                   MedicineDAO medicineDAO,
                                   AuditService auditService) {
        this.emergencyRequestDAO = emergencyRequestDAO;
        this.facilityDAO = facilityDAO;
        this.medicineDAO = medicineDAO;
        this.auditService = auditService;
    }

    /**
     * Creates a new emergency request after server-side validation.
     * Facility is derived from authenticated session, never from browser input.
     *
     * @return the created request with generated ID, or throws on validation failure
     */
    public EmergencyRequest createRequest(EmergencyRequest request, Long userId, Long facilityId,
                                          HttpServletRequest httpReq) {
        // Validate facility exists and is active
        Facility facility = facilityDAO.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        if (!facility.isActive()) {
            throw new IllegalArgumentException("Facility is not active");
        }

        // Enforce facility from session — never trust client-supplied facility ID
        request.setRequestingFacilityId(facilityId);
        request.setCreatedBy(userId);

        // Validate medicine exists
        Medicine medicine = medicineDAO.findById(request.getMedicineId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        // Validate quantity
        if (request.getQuantityNeeded() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Validate required-by date
        if (request.getRequiredByDate() == null) {
            throw new IllegalArgumentException("Required-by date is required");
        }
        if (request.getRequiredByDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Required-by date cannot be in the past");
        }

        // Validate urgency
        if (request.getUrgency() == null) {
            throw new IllegalArgumentException("Urgency level is required");
        }

        // Set defaults
        request.setStatus(RequestStatus.OPEN);
        request.setQuantityFulfilled(0);

        Long id = emergencyRequestDAO.create(request);
        request.setId(id);
        request.setFacilityName(facility.getName());
        request.setMedicineName(medicine.getName());

        logger.info("Emergency request created: id={}, facility={}, medicine={}, qty={}",
                id, facilityId, request.getMedicineId(), request.getQuantityNeeded());

        auditService.logAction(userId, "CREATE_EMERGENCY_REQUEST", "EmergencyRequest",
                id.toString(), httpReq);

        return request;
    }

    public Optional<EmergencyRequest> findById(Long id) {
        return emergencyRequestDAO.findById(id);
    }

    public List<EmergencyRequest> findByFacility(Long facilityId, int offset, int limit, String statusFilter) {
        return emergencyRequestDAO.findByFacilityId(facilityId, offset, limit, statusFilter);
    }

    public List<EmergencyRequest> findAll(int offset, int limit, String statusFilter) {
        return emergencyRequestDAO.findAll(offset, limit, statusFilter);
    }

    public int countByFacility(Long facilityId, String statusFilter) {
        return emergencyRequestDAO.countByFacilityId(facilityId, statusFilter);
    }

    public int countAll(String statusFilter) {
        return emergencyRequestDAO.countAll(statusFilter);
    }

    public int countOpenByFacility(Long facilityId) {
        return emergencyRequestDAO.countOpenByFacilityId(facilityId);
    }

    /**
     * Cancels a request. Only OPEN or PARTIALLY_FULFILLED requests can be cancelled.
     * Ownership is enforced: user must belong to the requesting facility or be ADMIN.
     */
    public void cancelRequest(Long requestId, Long userId, Long userFacilityId,
                              boolean isAdmin, HttpServletRequest httpReq) {
        EmergencyRequest request = emergencyRequestDAO.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Authorization: must own the request or be admin
        if (!isAdmin && !request.getRequestingFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Not authorized to cancel this request");
        }

        // Only OPEN or PARTIALLY_FULFILLED can be cancelled
        if (request.getStatus() != RequestStatus.OPEN &&
            request.getStatus() != RequestStatus.PARTIALLY_FULFILLED) {
            throw new IllegalStateException(
                    "Cannot cancel request in status: " + request.getStatus());
        }

        emergencyRequestDAO.updateStatus(requestId, RequestStatus.CANCELLED);

        logger.info("Emergency request cancelled: id={}, by user={}", requestId, userId);
        auditService.logAction(userId, "CANCEL_EMERGENCY_REQUEST", "EmergencyRequest",
                requestId.toString(), request.getStatus().name(), RequestStatus.CANCELLED.name(), httpReq);
    }
}
