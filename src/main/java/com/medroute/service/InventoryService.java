package com.medroute.service;

import com.medroute.dao.InventoryBatchDAO;
import com.medroute.dao.InventoryTransactionDAO;
import com.medroute.dao.StockConsumptionDAO;
import com.medroute.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryBatchDAO inventoryBatchDAO;
    private final InventoryTransactionDAO inventoryTransactionDAO;
    private final StockConsumptionDAO stockConsumptionDAO;
    private final AuditService auditService;

    public InventoryService(InventoryBatchDAO inventoryBatchDAO,
                            InventoryTransactionDAO inventoryTransactionDAO,
                            StockConsumptionDAO stockConsumptionDAO,
                            AuditService auditService) {
        this.inventoryBatchDAO = inventoryBatchDAO;
        this.inventoryTransactionDAO = inventoryTransactionDAO;
        this.stockConsumptionDAO = stockConsumptionDAO;
        this.auditService = auditService;
    }

    public String calculateStatus(InventoryBatch batch) {
        if (batch.getAvailableQuantity() <= 0) {
            return "DEPLETED";
        }
        
        if (batch.getExpiryDate() != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
            if (daysToExpiry < 0) return "EXPIRED";
            if (daysToExpiry <= 7) return "EXPIRY_CRITICAL";
            if (daysToExpiry <= 30) return "EXPIRY_WARNING";
        }
        
        int available = batch.getAvailableQuantity();
        if (available <= (batch.getMinimumStock() / 2.0)) return "CRITICAL";
        if (available <= batch.getMinimumStock()) return "LOW";
        if (available > batch.getTargetStock()) return "EXCESS";
        
        return "NORMAL";
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockIn(InventoryBatch batch, Long performedBy, HttpServletRequest request) {
        if (batch.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        
        // Save batch
        Long batchId = inventoryBatchDAO.create(batch);
        batch.setId(batchId);
        
        // Log transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setBatchId(batchId);
        tx.setFacilityId(batch.getFacilityId());
        tx.setMedicineId(batch.getMedicineId());
        tx.setTransactionType(TransactionType.STOCK_IN);
        tx.setQuantity(batch.getQuantity());
        tx.setPerformedBy(performedBy);
        tx.setNotes("Initial stock-in");
        inventoryTransactionDAO.create(tx);
        
        auditService.logAction(performedBy, "STOCK_IN", "InventoryBatch", batchId.toString(), request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeStock(Long facilityId, Long medicineId, int quantityToConsume, String department, Long performedBy, HttpServletRequest request) {
        if (quantityToConsume <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Get batches ordered by expiry
        List<InventoryBatch> batches = inventoryBatchDAO.findByFacilityId(facilityId, 0, 1000, null, null, null, null)
                .stream()
                .filter(b -> b.getMedicineId().equals(medicineId))
                .filter(b -> b.getAvailableQuantity() > 0)
                .filter(b -> b.getExpiryDate() == null || !b.getExpiryDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        int totalAvailable = batches.stream().mapToInt(InventoryBatch::getAvailableQuantity).sum();
        if (totalAvailable < quantityToConsume) {
            throw new IllegalStateException("Insufficient stock available");
        }

        int remainingToConsume = quantityToConsume;

        for (InventoryBatch batch : batches) {
            if (remainingToConsume <= 0) break;

            int availableInBatch = batch.getAvailableQuantity();
            int toDeduct = Math.min(availableInBatch, remainingToConsume);

            batch.setQuantity(batch.getQuantity() - toDeduct);
            if (batch.getAvailableQuantity() == 0) {
                batch.setStatus(BatchStatus.DEPLETED);
            }
            inventoryBatchDAO.update(batch);

            // Log consumption tx
            InventoryTransaction tx = new InventoryTransaction();
            tx.setBatchId(batch.getId());
            tx.setFacilityId(facilityId);
            tx.setMedicineId(medicineId);
            tx.setTransactionType(TransactionType.CONSUMPTION);
            tx.setQuantity(toDeduct);
            tx.setPerformedBy(performedBy);
            inventoryTransactionDAO.create(tx);

            remainingToConsume -= toDeduct;
        }

        // Create stock consumption record
        StockConsumption consumption = new StockConsumption();
        consumption.setFacilityId(facilityId);
        consumption.setMedicineId(medicineId);
        consumption.setQuantity(quantityToConsume);
        consumption.setDepartment(department);
        consumption.setRecordedBy(performedBy);
        stockConsumptionDAO.create(consumption);
        
        auditService.logAction(performedBy, "CONSUME_STOCK", "Medicine", medicineId.toString(), request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long batchId, Long facilityId, int newQuantity, Long performedBy, String notes, HttpServletRequest request) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        InventoryBatch batch = inventoryBatchDAO.findById(batchId);
        if (batch == null || !batch.getFacilityId().equals(facilityId)) {
            throw new IllegalArgumentException("Invalid batch or access denied");
        }

        int oldQuantity = batch.getQuantity();
        batch.setQuantity(newQuantity);
        
        if (batch.getAvailableQuantity() == 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        } else if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            batch.setStatus(BatchStatus.EXPIRED);
        } else {
            batch.setStatus(BatchStatus.ACTIVE);
        }
        
        inventoryBatchDAO.update(batch);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setBatchId(batchId);
        tx.setFacilityId(facilityId);
        tx.setMedicineId(batch.getMedicineId());
        tx.setTransactionType(TransactionType.ADJUSTMENT);
        tx.setQuantity(newQuantity - oldQuantity);
        tx.setPerformedBy(performedBy);
        tx.setNotes(notes);
        inventoryTransactionDAO.create(tx);

        auditService.logAction(performedBy, "ADJUST_STOCK", "InventoryBatch", batchId.toString(), request);
    }
    
    public Map<String, Object> getDashboardStats(Long facilityId) {
        Map<String, Object> stats = new HashMap<>();
        List<InventoryBatch> batches = inventoryBatchDAO.findByFacilityId(facilityId, 0, 10000, null, null, null, null);
        
        long totalItems = batches.size();
        long expired = batches.stream().filter(b -> calculateStatus(b).equals("EXPIRED")).count();
        long expiring = batches.stream().filter(b -> calculateStatus(b).equals("EXPIRY_CRITICAL") || calculateStatus(b).equals("EXPIRY_WARNING")).count();
        long critical = batches.stream().filter(b -> calculateStatus(b).equals("CRITICAL")).count();
        long low = batches.stream().filter(b -> calculateStatus(b).equals("LOW")).count();
        long excess = batches.stream().filter(b -> calculateStatus(b).equals("EXCESS")).count();
        
        stats.put("totalItems", totalItems);
        stats.put("expired", expired);
        stats.put("expiring", expiring);
        stats.put("critical", critical);
        stats.put("low", low);
        stats.put("excess", excess);
        
        return stats;
    }

    public List<InventoryBatch> findByFacilityId(Long facilityId, int offset, int limit, String search, Long categoryId, String status, String expiryRange) {
        return inventoryBatchDAO.findByFacilityId(facilityId, offset, limit, search, categoryId, status, expiryRange);
    }
    
    public int countByFacilityId(Long facilityId, String search, Long categoryId, String status, String expiryRange) {
        return inventoryBatchDAO.countByFacilityId(facilityId, search, categoryId, status, expiryRange);
    }

    public InventoryBatch findById(Long id) {
        return inventoryBatchDAO.findById(id);
    }
    
    public List<InventoryTransaction> getTransactionsForBatch(Long batchId) {
        return inventoryTransactionDAO.findByBatchId(batchId);
    }
}
