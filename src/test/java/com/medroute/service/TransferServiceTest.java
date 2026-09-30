package com.medroute.service;

import com.medroute.dao.*;
import com.medroute.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for TransferService — Phase 6B transactional stock transfer engine.
 *
 * Covers: state machine, FIFO reservation, dispatch concurrency re-locking,
 * receive with batch metadata, cancellation preserving items, emergency fulfillment.
 */
class TransferServiceTest {

    @Mock private TransferRequestDAO transferRequestDAO;
    @Mock private TransferItemDAO transferItemDAO;
    @Mock private InventoryBatchDAO inventoryBatchDAO;
    @Mock private InventoryTransactionDAO inventoryTransactionDAO;
    @Mock private EmergencyRequestDAO emergencyRequestDAO;
    @Mock private FacilityDAO facilityDAO;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private HttpServletRequest httpReq;

    @InjectMocks
    private TransferService transferService;

    // Test data constants
    private static final Long DONOR_FACILITY = 2L;
    private static final Long DEST_FACILITY = 5L;
    private static final Long ER_ID = 10L;
    private static final Long MEDICINE_ID = 3L;
    private static final Long USER_ID = 1L;
    private static final Long TRANSFER_ID = 100L;

    private EmergencyRequest emergencyRequest;
    private Facility donorFacility;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emergencyRequest = new EmergencyRequest();
        emergencyRequest.setId(ER_ID);
        emergencyRequest.setRequestingFacilityId(DEST_FACILITY);
        emergencyRequest.setMedicineId(MEDICINE_ID);
        emergencyRequest.setQuantityNeeded(1000);
        emergencyRequest.setQuantityFulfilled(0);
        emergencyRequest.setStatus(RequestStatus.OPEN);

        donorFacility = new Facility();
        donorFacility.setId(DONOR_FACILITY);
        donorFacility.setName("Apollo Hospital");
    }

    // ========================================================================
    // CREATE TRANSFER TESTS
    // ========================================================================

    @Test
    void testCreateTransfer_successful() {
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));
        when(facilityDAO.findById(DONOR_FACILITY)).thenReturn(Optional.of(donorFacility));
        when(transferRequestDAO.create(any())).thenReturn(TRANSFER_ID);

        TransferRequest result = transferService.createTransfer(
                DONOR_FACILITY, ER_ID, 200, BigDecimal.valueOf(85.0),
                BigDecimal.valueOf(15.5), USER_ID, DEST_FACILITY, httpReq);

        assertNotNull(result);
        assertEquals(TRANSFER_ID, result.getId());
        assertEquals(TransferStatus.REQUESTED, result.getStatus());
        assertEquals(200, result.getRequestedQuantity());

        verify(transferRequestDAO).create(any(TransferRequest.class));
        verify(auditService).logAction(eq(USER_ID), eq("TRANSFER_CREATED"), anyString(), anyString(), eq(httpReq));
        verify(notificationService).notifyFacility(eq(DONOR_FACILITY), eq(NotificationType.TRANSFER_REQUEST),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testCreateTransfer_quantityExceedsRemaining_throws() {
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.createTransfer(DONOR_FACILITY, ER_ID, 1500,
                        BigDecimal.ZERO, BigDecimal.ZERO, USER_ID, DEST_FACILITY, httpReq));
    }

    @Test
    void testCreateTransfer_zeroQuantity_throws() {
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.createTransfer(DONOR_FACILITY, ER_ID, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, USER_ID, DEST_FACILITY, httpReq));
    }

    @Test
    void testCreateTransfer_wrongFacility_securityException() {
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        // User facility is 99, not DEST_FACILITY (5)
        assertThrows(SecurityException.class, () ->
                transferService.createTransfer(DONOR_FACILITY, ER_ID, 200,
                        BigDecimal.ZERO, BigDecimal.ZERO, USER_ID, 99L, httpReq));
    }

    @Test
    void testCreateTransfer_closedEmergencyRequest_throws() {
        emergencyRequest.setStatus(RequestStatus.FULFILLED);
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        assertThrows(IllegalStateException.class, () ->
                transferService.createTransfer(DONOR_FACILITY, ER_ID, 200,
                        BigDecimal.ZERO, BigDecimal.ZERO, USER_ID, DEST_FACILITY, httpReq));
    }

    @Test
    void testCreateTransfer_selfTransfer_throws() {
        emergencyRequest.setRequestingFacilityId(DONOR_FACILITY); // same facility
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.createTransfer(DONOR_FACILITY, ER_ID, 200,
                        BigDecimal.ZERO, BigDecimal.ZERO, USER_ID, DONOR_FACILITY, httpReq));
    }

    // ========================================================================
    // ACCEPT (RESERVE) TESTS
    // ========================================================================

    @Test
    void testAccept_singleBatch_successful() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        InventoryBatch batch = makeBatch(1L, 500, 0, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));
        when(transferItemDAO.create(any())).thenReturn(1L);

        transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        // Verify batch reserved_quantity increased
        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO).update(batchCaptor.capture());
        assertEquals(200, batchCaptor.getValue().getReservedQuantity());

        // Verify transfer item created
        verify(transferItemDAO).create(any(TransferItem.class));

        // Verify status updated
        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.ACCEPTED);
        verify(transferRequestDAO).updateApprovedBy(TRANSFER_ID, USER_ID);
    }

    @Test
    void testAccept_multiBatch_FIFO_earliestExpiryFirst() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 300);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        // Two batches: early expiry (100 avail), later expiry (500 avail)
        InventoryBatch earlyBatch = makeBatch(1L, 100, 0, LocalDate.now().plusMonths(1));
        InventoryBatch lateBatch = makeBatch(2L, 500, 0, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(earlyBatch, lateBatch)); // Already FIFO ordered
        when(transferItemDAO.create(any())).thenReturn(1L, 2L);

        transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        // Verify both batches updated: early=100 reserved (all), late=200 reserved (partial)
        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO, times(2)).update(batchCaptor.capture());
        List<InventoryBatch> captured = batchCaptor.getAllValues();
        assertEquals(100, captured.get(0).getReservedQuantity()); // earlyBatch: all 100
        assertEquals(200, captured.get(1).getReservedQuantity()); // lateBatch: 200 of 500

        // Two transfer items created
        verify(transferItemDAO, times(2)).create(any(TransferItem.class));
    }

    @Test
    void testAccept_insufficientStock_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 500);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        // Only 100 available
        InventoryBatch batch = makeBatch(1L, 100, 0, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        // Should throw — transaction rollback undoes any partial reservation
        assertThrows(IllegalStateException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));

        // Status should NOT be updated
        verify(transferRequestDAO, never()).updateStatus(any(), any());
    }

    @Test
    void testAccept_reservedQuantityReducesAvailable() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        // Batch has 500 qty but 400 reserved → only 100 available
        InventoryBatch batch = makeBatch(1L, 500, 400, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        assertThrows(IllegalStateException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testAccept_expiredBatchesSkipped() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 100);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        // The FOR UPDATE query already filters expired batches, so empty result
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testAccept_wrongFacility_securityException() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(SecurityException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, 99L, httpReq));
    }

    @Test
    void testAccept_invalidStatus_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testAccept_duplicateAccept_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.acceptTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    // ========================================================================
    // REJECT TESTS
    // ========================================================================

    @Test
    void testReject_fromRequested() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        transferService.rejectTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.REJECTED);
        verify(inventoryBatchDAO, never()).update(any()); // No inventory effect
    }

    @Test
    void testReject_wrongFacility_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(SecurityException.class, () ->
                transferService.rejectTransfer(TRANSFER_ID, USER_ID, 99L, httpReq));
    }

    @Test
    void testReject_invalidStatus_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.rejectTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    // ========================================================================
    // SCHEDULE TESTS
    // ========================================================================

    @Test
    void testSchedule_fromAccepted() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        transferService.scheduleTransfer(TRANSFER_ID, "Ship Monday", USER_ID, DONOR_FACILITY, httpReq);

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.SCHEDULED);
        verify(transferRequestDAO).updateNotes(TRANSFER_ID, "Ship Monday");
    }

    @Test
    void testSchedule_invalidStatus_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.scheduleTransfer(TRANSFER_ID, "notes", USER_ID, DONOR_FACILITY, httpReq));
    }

    // ========================================================================
    // DISPATCH TESTS — CRITICAL CONCURRENCY
    // ========================================================================

    @Test
    void testDispatch_fromAccepted_successful() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        InventoryBatch batch = makeBatch(10L, 500, 200, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        // Verify batch deducted
        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO, atLeastOnce()).update(batchCaptor.capture());
        InventoryBatch updated = batchCaptor.getValue();
        assertEquals(300, updated.getQuantity()); // 500 - 200
        assertEquals(0, updated.getReservedQuantity()); // 200 - 200

        // Verify TRANSFERRED_OUT transaction
        verify(inventoryTransactionDAO).create(argThat(tx ->
                tx.getTransactionType() == TransactionType.TRANSFERRED_OUT
                && tx.getQuantity() == 200));

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.IN_TRANSIT);
    }

    @Test
    void testDispatch_fromScheduled() {
        TransferRequest tr = makeTransfer(TransferStatus.SCHEDULED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        InventoryBatch batch = makeBatch(10L, 500, 200, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.IN_TRANSIT);
    }

    @Test
    void testDispatch_invalidStatus_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_batchDepleted_statusChanges() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 500);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 500);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        // Batch has exactly 500 qty, 500 reserved → depleted after dispatch
        InventoryBatch batch = makeBatch(10L, 500, 500, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq);

        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO, atLeastOnce()).update(batchCaptor.capture());
        assertEquals(0, batchCaptor.getValue().getQuantity());
        assertEquals(BatchStatus.DEPLETED, batchCaptor.getValue().getStatus());
    }

    @Test
    void testDispatch_concurrentInventoryChange_insufficientQuantity_throws() {
        // Simulates: inventory was consumed between accept and dispatch
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        // First findById returns batch with 200 reserved
        // But after FOR UPDATE re-lock, quantity has dropped to 100 (consumed externally)
        InventoryBatch stale = makeBatch(10L, 200, 200, LocalDate.now().plusMonths(6));
        InventoryBatch current = makeBatch(10L, 100, 200, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(stale, current);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(current));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_batchExpired_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        // Batch expired between accept and dispatch
        InventoryBatch batch = makeBatch(10L, 500, 200, LocalDate.now().minusDays(1));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_batchStatusNotActive_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        // Batch status changed to DEPLETED between accept and dispatch
        InventoryBatch batch = makeBatch(10L, 500, 200, LocalDate.now().plusMonths(6));
        batch.setStatus(BatchStatus.DEPLETED);
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_reservationInsufficient_throws() {
        // Reservation was partially released by another cancel
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        // Reserved only 50 but item says 200
        InventoryBatch batch = makeBatch(10L, 500, 50, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_duplicateDispatch_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testDispatch_multiBatch_rollbackOnFailure() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 300);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item1 = makeItem(1L, 10L, 100);
        TransferItem item2 = makeItem(2L, 20L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item1, item2));

        InventoryBatch batch1 = makeBatch(10L, 200, 100, LocalDate.now().plusMonths(6));
        // Batch 2 has insufficient reserved qty — will fail
        InventoryBatch batch2 = makeBatch(20L, 300, 50, LocalDate.now().plusMonths(6));

        when(inventoryBatchDAO.findById(10L)).thenReturn(batch1);
        when(inventoryBatchDAO.findById(20L)).thenReturn(batch2);
        when(inventoryBatchDAO.findForUpdateByFacilityAndMedicine(DONOR_FACILITY, MEDICINE_ID))
                .thenReturn(List.of(batch1, batch2));

        // Should throw on second batch — @Transactional would roll back both
        assertThrows(IllegalStateException.class, () ->
                transferService.dispatchTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));

        // Status NOT updated (would rollback in real TX)
        verify(transferRequestDAO, never()).updateStatus(any(), eq(TransferStatus.IN_TRANSIT));
    }

    // ========================================================================
    // RECEIVE TESTS
    // ========================================================================

    @Test
    void testReceive_successful_createsDestinationBatch() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        InventoryBatch sourceBatch = makeBatch(10L, 300, 0, LocalDate.now().plusMonths(6));
        sourceBatch.setManufactureDate(LocalDate.of(2025, 1, 1));
        sourceBatch.setUnitPrice(BigDecimal.valueOf(25.50));
        sourceBatch.setSupplier("PharmaCorp");
        when(inventoryBatchDAO.findById(10L)).thenReturn(sourceBatch);
        when(inventoryBatchDAO.create(any())).thenReturn(99L);

        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        transferService.receiveTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, httpReq);

        // Verify destination batch created with source metadata
        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO).create(batchCaptor.capture());
        InventoryBatch destBatch = batchCaptor.getValue();
        assertEquals(DEST_FACILITY, destBatch.getFacilityId());
        assertEquals(MEDICINE_ID, destBatch.getMedicineId());
        assertEquals(200, destBatch.getQuantity());
        assertEquals(0, destBatch.getReservedQuantity());
        assertEquals(BatchStatus.ACTIVE, destBatch.getStatus());
        assertTrue(destBatch.getBatchNumber().startsWith("TXF-"));
        // Source metadata preserved
        assertEquals(sourceBatch.getExpiryDate(), destBatch.getExpiryDate());
        assertEquals(sourceBatch.getManufactureDate(), destBatch.getManufactureDate());
        assertEquals(sourceBatch.getUnitPrice(), destBatch.getUnitPrice());
        assertEquals("PharmaCorp", destBatch.getSupplier());

        // TRANSFERRED_IN transaction created
        verify(inventoryTransactionDAO).create(argThat(tx ->
                tx.getTransactionType() == TransactionType.TRANSFERRED_IN
                && tx.getQuantity() == 200
                && tx.getFacilityId().equals(DEST_FACILITY)));

        // Status set to COMPLETED
        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.COMPLETED);
    }

    @Test
    void testReceive_partialEmergencyFulfillment() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 200);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        InventoryBatch sourceBatch = makeBatch(10L, 300, 0, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(sourceBatch);
        when(inventoryBatchDAO.create(any())).thenReturn(99L);

        // ER needs 1000, fulfilling 200 → PARTIALLY_FULFILLED
        emergencyRequest.setQuantityNeeded(1000);
        emergencyRequest.setQuantityFulfilled(0);
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        transferService.receiveTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, httpReq);

        verify(emergencyRequestDAO).updateQuantityFulfilled(ER_ID, 200);
        verify(emergencyRequestDAO).updateStatus(ER_ID, RequestStatus.PARTIALLY_FULFILLED);
    }

    @Test
    void testReceive_exactEmergencyFulfillment() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 500);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item = makeItem(1L, 10L, 500);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item));

        InventoryBatch sourceBatch = makeBatch(10L, 500, 0, LocalDate.now().plusMonths(6));
        when(inventoryBatchDAO.findById(10L)).thenReturn(sourceBatch);
        when(inventoryBatchDAO.create(any())).thenReturn(99L);

        // ER needs 1000, already fulfilled 500, this 500 completes it
        emergencyRequest.setQuantityNeeded(1000);
        emergencyRequest.setQuantityFulfilled(500);
        when(emergencyRequestDAO.findById(ER_ID)).thenReturn(Optional.of(emergencyRequest));

        transferService.receiveTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, httpReq);

        verify(emergencyRequestDAO).updateQuantityFulfilled(ER_ID, 1000);
        verify(emergencyRequestDAO).updateStatus(ER_ID, RequestStatus.FULFILLED);
    }

    @Test
    void testReceive_wrongFacility_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(SecurityException.class, () ->
                transferService.receiveTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, httpReq));
    }

    @Test
    void testReceive_duplicateReceive_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.COMPLETED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.receiveTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, httpReq));
    }

    // ========================================================================
    // CANCEL TESTS
    // ========================================================================

    @Test
    void testCancel_fromRequested_noReservationEffect() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        transferService.cancelTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, false, httpReq);

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.CANCELLED);
        // No inventory operations
        verify(inventoryBatchDAO, never()).update(any());
        verify(inventoryBatchDAO, never()).findById(any());
    }

    @Test
    void testCancel_fromAccepted_releasesReservation_preservesItems() {
        TransferRequest tr = makeTransfer(TransferStatus.ACCEPTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        TransferItem item1 = makeItem(1L, 10L, 100);
        TransferItem item2 = makeItem(2L, 20L, 100);
        when(transferItemDAO.findByTransferId(TRANSFER_ID)).thenReturn(List.of(item1, item2));

        InventoryBatch batch1 = makeBatch(10L, 500, 200, LocalDate.now().plusMonths(6));
        InventoryBatch batch2 = makeBatch(20L, 300, 150, LocalDate.now().plusMonths(3));
        when(inventoryBatchDAO.findById(10L)).thenReturn(batch1);
        when(inventoryBatchDAO.findById(20L)).thenReturn(batch2);

        transferService.cancelTransfer(TRANSFER_ID, USER_ID, DONOR_FACILITY, false, httpReq);

        // Verify reservation released
        ArgumentCaptor<InventoryBatch> batchCaptor = ArgumentCaptor.forClass(InventoryBatch.class);
        verify(inventoryBatchDAO, times(2)).update(batchCaptor.capture());
        assertEquals(100, batchCaptor.getAllValues().get(0).getReservedQuantity()); // 200-100
        assertEquals(50, batchCaptor.getAllValues().get(1).getReservedQuantity()); // 150-100

        // Transfer items NOT deleted — they are preserved for audit trail
        // (No deleteByTransferId method is called)

        // STOCK_RELEASED audit for each batch
        verify(auditService, atLeast(2)).logAction(eq(USER_ID), eq("STOCK_RELEASED"),
                eq("InventoryBatch"), anyString(), anyString(), anyString(), eq(httpReq));

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.CANCELLED);
    }

    @Test
    void testCancel_fromInTransit_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.IN_TRANSIT, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.cancelTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, false, httpReq));
    }

    @Test
    void testCancel_terminalStatus_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.COMPLETED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        assertThrows(IllegalStateException.class, () ->
                transferService.cancelTransfer(TRANSFER_ID, USER_ID, DEST_FACILITY, false, httpReq));
    }

    @Test
    void testCancel_unauthorizedUser_throws() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        // User from facility 99, not donor or dest
        assertThrows(SecurityException.class, () ->
                transferService.cancelTransfer(TRANSFER_ID, USER_ID, 99L, false, httpReq));
    }

    @Test
    void testCancel_adminCanCancel() {
        TransferRequest tr = makeTransfer(TransferStatus.REQUESTED, 200);
        when(transferRequestDAO.findById(TRANSFER_ID)).thenReturn(Optional.of(tr));

        // Admin from unrelated facility can still cancel
        transferService.cancelTransfer(TRANSFER_ID, USER_ID, 99L, true, httpReq);

        verify(transferRequestDAO).updateStatus(TRANSFER_ID, TransferStatus.CANCELLED);
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private TransferRequest makeTransfer(TransferStatus status, int requestedQty) {
        TransferRequest tr = new TransferRequest();
        tr.setId(TRANSFER_ID);
        tr.setFromFacilityId(DONOR_FACILITY);
        tr.setToFacilityId(DEST_FACILITY);
        tr.setEmergencyRequestId(ER_ID);
        tr.setRequestedQuantity(requestedQty);
        tr.setStatus(status);
        tr.setMatchScore(BigDecimal.valueOf(85.0));
        tr.setDistanceKm(BigDecimal.valueOf(15.5));
        tr.setRequestedBy(USER_ID);
        tr.setCreatedAt(LocalDateTime.now());
        return tr;
    }

    private InventoryBatch makeBatch(Long id, int qty, int reserved, LocalDate expiry) {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(id);
        batch.setFacilityId(DONOR_FACILITY);
        batch.setMedicineId(MEDICINE_ID);
        batch.setBatchNumber("BATCH-" + id);
        batch.setQuantity(qty);
        batch.setReservedQuantity(reserved);
        batch.setExpiryDate(expiry);
        batch.setStatus(BatchStatus.ACTIVE);
        batch.setManufactureDate(LocalDate.of(2025, 1, 1));
        batch.setUnitPrice(BigDecimal.valueOf(10.0));
        batch.setSupplier("TestSupplier");
        return batch;
    }

    private TransferItem makeItem(Long id, Long batchId, int qty) {
        TransferItem item = new TransferItem();
        item.setId(id);
        item.setTransferId(TRANSFER_ID);
        item.setBatchId(batchId);
        item.setMedicineId(MEDICINE_ID);
        item.setQuantity(qty);
        return item;
    }
}
