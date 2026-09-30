package com.medroute.service;

import com.medroute.dao.*;
import com.medroute.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transactional Stock Transfer Engine.
 *
 * Manages the complete transfer lifecycle:
 * REQUESTED → ACCEPTED → SCHEDULED → IN_TRANSIT → RECEIVED → COMPLETED
 *
 * Guarantees:
 * - Inventory consistency via SELECT ... FOR UPDATE
 * - FIFO batch selection (earliest expiry first)
 * - Atomic reservation, dispatch, and receive
 * - Rollback on any failure
 * - Transfer items preserved on cancellation (audit trail)
 * - Emergency request fulfillment updates
 */
@Service
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final TransferRequestDAO transferRequestDAO;
    private final TransferItemDAO transferItemDAO;
    private final InventoryBatchDAO inventoryBatchDAO;
    private final InventoryTransactionDAO inventoryTransactionDAO;
    private final EmergencyRequestDAO emergencyRequestDAO;
    private final FacilityDAO facilityDAO;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public TransferService(TransferRequestDAO transferRequestDAO,
                           TransferItemDAO transferItemDAO,
                           InventoryBatchDAO inventoryBatchDAO,
                           InventoryTransactionDAO inventoryTransactionDAO,
                           EmergencyRequestDAO emergencyRequestDAO,
                           FacilityDAO facilityDAO,
                           AuditService auditService,
                           NotificationService notificationService) {
        this.transferRequestDAO = transferRequestDAO;
        this.transferItemDAO = transferItemDAO;
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.inventoryTransactionDAO = inventoryTransactionDAO;
        this.emergencyRequestDAO = emergencyRequestDAO;
        this.facilityDAO = facilityDAO;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    // ========================================================================
    // CREATE TRANSFER
    // ========================================================================

    /**
     * Creates a new transfer request from matching results.
     * No inventory reservation at this stage.
     *
     * @param fromFacilityId donor facility
     * @param emergencyRequestId the emergency request being addressed
     * @param requestedQuantity how much to request
     * @param matchScore from MatchCandidate
     * @param distanceKm from MatchCandidate
     * @param userId the requesting user
     * @param httpReq for audit
     * @return the created TransferRequest
     */
    @Transactional(rollbackFor = Exception.class)
    public TransferRequest createTransfer(Long fromFacilityId, Long emergencyRequestId,
                                          int requestedQuantity, BigDecimal matchScore,
                                          BigDecimal distanceKm, Long userId, Long userFacilityId,
                                          HttpServletRequest httpReq) {
        // Validate emergency request
        EmergencyRequest er = emergencyRequestDAO.findById(emergencyRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency request not found"));

        if (er.getStatus() != RequestStatus.OPEN && er.getStatus() != RequestStatus.PARTIALLY_FULFILLED) {
            throw new IllegalStateException("Emergency request is not open for transfers: " + er.getStatus());
        }

        // Authorization: user must belong to the requesting (destination) facility
        if (!er.getRequestingFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Not authorized to create transfer for this emergency request");
        }

        // Validate quantity
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        int remaining = er.getRemainingQuantity();
        if (requestedQuantity > remaining) {
            throw new IllegalArgumentException("Requested quantity (" + requestedQuantity +
                    ") exceeds remaining need (" + remaining + ")");
        }

        // Validate source facility
        facilityDAO.findById(fromFacilityId)
                .orElseThrow(() -> new IllegalArgumentException("Source facility not found"));

        if (fromFacilityId.equals(er.getRequestingFacilityId())) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }

        // Create transfer request
        TransferRequest tr = new TransferRequest();
        tr.setFromFacilityId(fromFacilityId);
        tr.setToFacilityId(er.getRequestingFacilityId());
        tr.setEmergencyRequestId(emergencyRequestId);
        tr.setRequestedQuantity(requestedQuantity);
        tr.setStatus(TransferStatus.REQUESTED);
        tr.setMatchScore(matchScore);
        tr.setDistanceKm(distanceKm);
        tr.setRequestedBy(userId);

        Long transferId = transferRequestDAO.create(tr);
        tr.setId(transferId);

        auditService.logAction(userId, "TRANSFER_CREATED", "TransferRequest",
                transferId.toString(), httpReq);

        // Notify donor facility
        notificationService.notifyFacility(fromFacilityId, NotificationType.TRANSFER_REQUEST,
                "New Transfer Request",
                "A transfer of " + requestedQuantity + " units has been requested.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer created: id={}, from={}, to={}, qty={}",
                transferId, fromFacilityId, er.getRequestingFacilityId(), requestedQuantity);

        return tr;
    }

    // ========================================================================
    // ACCEPT — Reserve inventory (FIFO, FOR UPDATE)
    // ========================================================================

    /**
     * Donor accepts a transfer request. Atomically reserves inventory using FIFO.
     * Uses SELECT ... FOR UPDATE for concurrency safety.
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptTransfer(Long transferId, Long userId, Long userFacilityId,
                               HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        if (tr.getStatus() != TransferStatus.REQUESTED) {
            throw new IllegalStateException("Cannot accept transfer in status: " + tr.getStatus());
        }
        if (!tr.getFromFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Only the donor facility can accept this transfer");
        }

        // Get the medicine ID from the emergency request
        EmergencyRequest er = emergencyRequestDAO.findById(tr.getEmergencyRequestId())
                .orElseThrow(() -> new IllegalStateException("Emergency request not found"));

        // Lock and reserve batches — FIFO by expiry
        List<InventoryBatch> lockedBatches = inventoryBatchDAO
                .findForUpdateByFacilityAndMedicine(tr.getFromFacilityId(), er.getMedicineId());

        int remaining = tr.getRequestedQuantity();
        List<TransferItem> items = new ArrayList<>();

        for (InventoryBatch batch : lockedBatches) {
            if (remaining <= 0) break;

            int available = batch.getAvailableQuantity(); // quantity - reservedQuantity
            if (available <= 0) continue;

            int allocation = Math.min(available, remaining);

            // Reserve
            batch.setReservedQuantity(batch.getReservedQuantity() + allocation);
            inventoryBatchDAO.update(batch);

            // Create transfer item
            TransferItem item = new TransferItem();
            item.setTransferId(transferId);
            item.setBatchId(batch.getId());
            item.setMedicineId(er.getMedicineId());
            item.setQuantity(allocation);
            Long itemId = transferItemDAO.create(item);
            item.setId(itemId);
            items.add(item);

            auditService.logAction(userId, "STOCK_RESERVED", "InventoryBatch",
                    batch.getId().toString(),
                    String.valueOf(batch.getReservedQuantity() - allocation),
                    String.valueOf(batch.getReservedQuantity()),
                    httpReq);

            remaining -= allocation;
        }

        if (remaining > 0) {
            // Rollback will undo all reservations above
            throw new IllegalStateException("Insufficient stock to fulfill transfer. " +
                    "Short by " + remaining + " units.");
        }

        // Update transfer status
        transferRequestDAO.updateStatus(transferId, TransferStatus.ACCEPTED);
        transferRequestDAO.updateApprovedBy(transferId, userId);

        auditService.logAction(userId, "TRANSFER_ACCEPTED", "TransferRequest",
                transferId.toString(), "REQUESTED", "ACCEPTED", httpReq);

        // Notify destination
        notificationService.notifyFacility(tr.getToFacilityId(), NotificationType.TRANSFER_UPDATE,
                "Transfer Accepted",
                "Transfer #" + transferId + " has been accepted by the donor facility.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} accepted: {} items reserved", transferId, items.size());
    }

    // ========================================================================
    // REJECT
    // ========================================================================

    @Transactional(rollbackFor = Exception.class)
    public void rejectTransfer(Long transferId, Long userId, Long userFacilityId,
                               HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        if (tr.getStatus() != TransferStatus.REQUESTED) {
            throw new IllegalStateException("Cannot reject transfer in status: " + tr.getStatus());
        }
        if (!tr.getFromFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Only the donor facility can reject this transfer");
        }

        transferRequestDAO.updateStatus(transferId, TransferStatus.REJECTED);

        auditService.logAction(userId, "TRANSFER_REJECTED", "TransferRequest",
                transferId.toString(), "REQUESTED", "REJECTED", httpReq);

        notificationService.notifyFacility(tr.getToFacilityId(), NotificationType.TRANSFER_UPDATE,
                "Transfer Rejected",
                "Transfer #" + transferId + " has been rejected by the donor facility.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} rejected by user {}", transferId, userId);
    }

    // ========================================================================
    // SCHEDULE
    // ========================================================================

    @Transactional(rollbackFor = Exception.class)
    public void scheduleTransfer(Long transferId, String scheduleNotes, Long userId,
                                 Long userFacilityId, HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        if (tr.getStatus() != TransferStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot schedule transfer in status: " + tr.getStatus());
        }
        if (!tr.getFromFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Only the donor facility can schedule this transfer");
        }

        transferRequestDAO.updateStatus(transferId, TransferStatus.SCHEDULED);
        if (scheduleNotes != null && !scheduleNotes.isEmpty()) {
            transferRequestDAO.updateNotes(transferId, scheduleNotes);
        }

        auditService.logAction(userId, "TRANSFER_SCHEDULED", "TransferRequest",
                transferId.toString(), "ACCEPTED", "SCHEDULED", httpReq);

        notificationService.notifyFacility(tr.getToFacilityId(), NotificationType.TRANSFER_UPDATE,
                "Transfer Scheduled",
                "Transfer #" + transferId + " has been scheduled for dispatch.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} scheduled by user {}", transferId, userId);
    }

    // ========================================================================
    // DISPATCH — Deduct source inventory with FOR UPDATE re-lock
    // ========================================================================

    /**
     * Dispatches a transfer. Atomically:
     * 1. Re-acquires row locks on source batches (FOR UPDATE)
     * 2. Validates batch availability, status, and expiry
     * 3. Deducts quantity and reserved_quantity from source batches
     * 4. Creates TRANSFERRED_OUT inventory transactions
     * 5. Updates transfer status to IN_TRANSIT
     *
     * Any failure rolls back the entire dispatch.
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispatchTransfer(Long transferId, Long userId, Long userFacilityId,
                                 HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        if (tr.getStatus() != TransferStatus.ACCEPTED && tr.getStatus() != TransferStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot dispatch transfer in status: " + tr.getStatus());
        }
        if (!tr.getFromFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Only the donor facility can dispatch this transfer");
        }

        List<TransferItem> items = transferItemDAO.findByTransferId(transferId);
        if (items.isEmpty()) {
            throw new IllegalStateException("No transfer items found — cannot dispatch");
        }

        String previousStatus = tr.getStatus().name();

        // Re-acquire locks and validate each batch
        for (TransferItem item : items) {
            // Lock the specific batch row
            InventoryBatch batch = inventoryBatchDAO.findById(item.getBatchId());
            if (batch == null) {
                throw new IllegalStateException("Source batch " + item.getBatchId() + " no longer exists");
            }

            // Re-lock via FOR UPDATE on the facility+medicine (locks all related batches)
            // This is done once per unique facility+medicine combo but the lock covers our batch
            inventoryBatchDAO.findForUpdateByFacilityAndMedicine(
                    batch.getFacilityId(), batch.getMedicineId());

            // Re-read after lock acquisition to get latest values
            batch = inventoryBatchDAO.findById(item.getBatchId());

            // Validate batch is still viable
            if (batch.getStatus() != BatchStatus.ACTIVE) {
                throw new IllegalStateException("Batch " + batch.getBatchNumber() +
                        " is no longer ACTIVE (status: " + batch.getStatus() + ")");
            }
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalStateException("Batch " + batch.getBatchNumber() + " has expired");
            }
            if (batch.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Batch " + batch.getBatchNumber() +
                        " has insufficient quantity: " + batch.getQuantity() +
                        " < " + item.getQuantity());
            }
            if (batch.getReservedQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Batch " + batch.getBatchNumber() +
                        " has insufficient reservation: " + batch.getReservedQuantity() +
                        " < " + item.getQuantity());
            }

            // Deduct: quantity and reserved_quantity both decrease
            batch.setQuantity(batch.getQuantity() - item.getQuantity());
            batch.setReservedQuantity(batch.getReservedQuantity() - item.getQuantity());

            if (batch.getQuantity() == 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }

            inventoryBatchDAO.update(batch);

            // Create TRANSFERRED_OUT transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBatchId(item.getBatchId());
            tx.setFacilityId(tr.getFromFacilityId());
            tx.setMedicineId(item.getMedicineId());
            tx.setTransactionType(TransactionType.TRANSFERRED_OUT);
            tx.setQuantity(item.getQuantity());
            tx.setPerformedBy(userId);
            tx.setReferenceId(transferId.toString());
            tx.setNotes("Transfer #" + transferId + " dispatched");
            inventoryTransactionDAO.create(tx);
        }

        transferRequestDAO.updateStatus(transferId, TransferStatus.IN_TRANSIT);

        auditService.logAction(userId, "TRANSFER_DISPATCHED", "TransferRequest",
                transferId.toString(), previousStatus, "IN_TRANSIT", httpReq);

        notificationService.notifyFacility(tr.getToFacilityId(), NotificationType.TRANSFER_UPDATE,
                "Transfer Dispatched",
                "Transfer #" + transferId + " has been dispatched and is now in transit.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} dispatched: {} items shipped", transferId, items.size());
    }

    // ========================================================================
    // RECEIVE + COMPLETE — Atomic destination inventory increase + ER update
    // ========================================================================

    /**
     * Destination confirms receipt. Atomically:
     * 1. Creates new inventory batches at destination (preserving source batch metadata)
     * 2. Creates TRANSFERRED_IN inventory transactions
     * 3. Updates emergency request fulfillment
     * 4. Sets transfer status to COMPLETED
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveTransfer(Long transferId, Long userId, Long userFacilityId,
                                HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        if (tr.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new IllegalStateException("Cannot receive transfer in status: " + tr.getStatus());
        }
        if (!tr.getToFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Only the destination facility can receive this transfer");
        }

        List<TransferItem> items = transferItemDAO.findByTransferId(transferId);
        if (items.isEmpty()) {
            throw new IllegalStateException("No transfer items found — cannot receive");
        }

        int totalReceived = 0;

        for (TransferItem item : items) {
            // Get source batch for metadata (expiry, manufacture date, price, supplier)
            InventoryBatch sourceBatch = inventoryBatchDAO.findById(item.getBatchId());

            // Create new batch at destination
            InventoryBatch destBatch = new InventoryBatch();
            destBatch.setFacilityId(tr.getToFacilityId());
            destBatch.setMedicineId(item.getMedicineId());
            destBatch.setBatchNumber("TXF-" + transferId + "-" + item.getId());
            destBatch.setQuantity(item.getQuantity());
            destBatch.setReservedQuantity(0);
            destBatch.setMinimumStock(0);
            destBatch.setTargetStock(0);
            destBatch.setStatus(BatchStatus.ACTIVE);
            destBatch.setReceivedDate(LocalDate.now());

            // Preserve source batch traceability
            if (sourceBatch != null) {
                destBatch.setExpiryDate(sourceBatch.getExpiryDate());
                destBatch.setManufactureDate(sourceBatch.getManufactureDate());
                destBatch.setUnitPrice(sourceBatch.getUnitPrice());
                destBatch.setSupplier(sourceBatch.getSupplier());
            }

            Long destBatchId = inventoryBatchDAO.create(destBatch);

            // Create TRANSFERRED_IN transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBatchId(destBatchId);
            tx.setFacilityId(tr.getToFacilityId());
            tx.setMedicineId(item.getMedicineId());
            tx.setTransactionType(TransactionType.TRANSFERRED_IN);
            tx.setQuantity(item.getQuantity());
            tx.setPerformedBy(userId);
            tx.setReferenceId(transferId.toString());
            tx.setNotes("Transfer #" + transferId + " received from facility " + tr.getFromFacilityId());
            inventoryTransactionDAO.create(tx);

            totalReceived += item.getQuantity();
        }

        // Update emergency request fulfillment
        if (tr.getEmergencyRequestId() != null) {
            EmergencyRequest er = emergencyRequestDAO.findById(tr.getEmergencyRequestId())
                    .orElse(null);
            if (er != null) {
                int newFulfilled = Math.min(
                        er.getQuantityFulfilled() + totalReceived,
                        er.getQuantityNeeded());
                emergencyRequestDAO.updateQuantityFulfilled(er.getId(), newFulfilled);

                if (newFulfilled >= er.getQuantityNeeded()) {
                    emergencyRequestDAO.updateStatus(er.getId(), RequestStatus.FULFILLED);
                } else if (newFulfilled > 0) {
                    emergencyRequestDAO.updateStatus(er.getId(), RequestStatus.PARTIALLY_FULFILLED);
                }
            }
        }

        // Set COMPLETED (atomic with RECEIVED per approved design)
        transferRequestDAO.updateStatus(transferId, TransferStatus.COMPLETED);

        auditService.logAction(userId, "TRANSFER_RECEIVED", "TransferRequest",
                transferId.toString(), "IN_TRANSIT", "COMPLETED", httpReq);

        notificationService.notifyFacility(tr.getFromFacilityId(), NotificationType.TRANSFER_UPDATE,
                "Transfer Completed",
                "Transfer #" + transferId + " has been received and completed. " +
                        totalReceived + " units delivered.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} received and completed: {} units", transferId, totalReceived);
    }

    // ========================================================================
    // CANCEL — Release reservation if applicable (preserve transfer_items)
    // ========================================================================

    /**
     * Cancels a transfer. If inventory was reserved (ACCEPTED/SCHEDULED),
     * releases the reservation. Transfer items are PRESERVED for audit trail.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTransfer(Long transferId, Long userId, Long userFacilityId,
                               boolean isAdmin, HttpServletRequest httpReq) {
        TransferRequest tr = getAndValidateTransfer(transferId);

        // Cannot cancel terminal states or IN_TRANSIT
        if (tr.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel transfer in terminal status: " + tr.getStatus());
        }
        if (tr.getStatus() == TransferStatus.IN_TRANSIT) {
            throw new IllegalStateException("Cannot cancel transfer that is already in transit");
        }

        // Authorization: source, destination, or admin
        if (!isAdmin && !tr.getFromFacilityId().equals(userFacilityId)
                     && !tr.getToFacilityId().equals(userFacilityId)) {
            throw new SecurityException("Not authorized to cancel this transfer");
        }

        String previousStatus = tr.getStatus().name();

        // Release reservation if ACCEPTED or SCHEDULED
        if (tr.getStatus() == TransferStatus.ACCEPTED || tr.getStatus() == TransferStatus.SCHEDULED) {
            List<TransferItem> items = transferItemDAO.findByTransferId(transferId);

            for (TransferItem item : items) {
                InventoryBatch batch = inventoryBatchDAO.findById(item.getBatchId());
                if (batch != null && batch.getReservedQuantity() >= item.getQuantity()) {
                    int oldReserved = batch.getReservedQuantity();
                    batch.setReservedQuantity(batch.getReservedQuantity() - item.getQuantity());
                    inventoryBatchDAO.update(batch);

                    auditService.logAction(userId, "STOCK_RELEASED", "InventoryBatch",
                            batch.getId().toString(),
                            String.valueOf(oldReserved),
                            String.valueOf(batch.getReservedQuantity()),
                            httpReq);
                }
            }
            // Transfer items are NOT deleted — preserved for audit trail
        }

        transferRequestDAO.updateStatus(transferId, TransferStatus.CANCELLED);

        auditService.logAction(userId, "TRANSFER_CANCELLED", "TransferRequest",
                transferId.toString(), previousStatus, "CANCELLED", httpReq);

        // Notify the other party
        Long notifyFacilityId = tr.getFromFacilityId().equals(userFacilityId)
                ? tr.getToFacilityId() : tr.getFromFacilityId();
        notificationService.notifyFacility(notifyFacilityId, NotificationType.TRANSFER_UPDATE,
                "Transfer Cancelled",
                "Transfer #" + transferId + " has been cancelled.",
                transferId.toString(), "TransferRequest");

        logger.info("Transfer {} cancelled from status {} by user {}", transferId, previousStatus, userId);
    }

    // ========================================================================
    // QUERY METHODS
    // ========================================================================

    public Optional<TransferRequest> findById(Long id) {
        Optional<TransferRequest> opt = transferRequestDAO.findById(id);
        opt.ifPresent(tr -> tr.setItems(transferItemDAO.findByTransferId(tr.getId())));
        return opt;
    }

    public List<TransferRequest> findIncoming(Long facilityId, int offset, int limit, String statusFilter) {
        return transferRequestDAO.findIncoming(facilityId, offset, limit, statusFilter);
    }

    public List<TransferRequest> findOutgoing(Long facilityId, int offset, int limit, String statusFilter) {
        return transferRequestDAO.findOutgoing(facilityId, offset, limit, statusFilter);
    }

    public List<TransferRequest> findAll(int offset, int limit, String statusFilter) {
        return transferRequestDAO.findAll(offset, limit, statusFilter);
    }

    public int countIncoming(Long facilityId, String statusFilter) {
        return transferRequestDAO.countIncoming(facilityId, statusFilter);
    }

    public int countOutgoing(Long facilityId, String statusFilter) {
        return transferRequestDAO.countOutgoing(facilityId, statusFilter);
    }

    public int countAll(String statusFilter) {
        return transferRequestDAO.countAll(statusFilter);
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private TransferRequest getAndValidateTransfer(Long transferId) {
        return transferRequestDAO.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }
}
