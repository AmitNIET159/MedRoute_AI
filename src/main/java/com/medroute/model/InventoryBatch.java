package com.medroute.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class InventoryBatch {
    private Long id;
    private Long facilityId;
    private Long medicineId;
    private transient String medicineName;
    private String batchNumber;
    private int quantity;
    private int reservedQuantity;
    private int minimumStock;
    private int targetStock;
    private BigDecimal unitPrice;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private LocalDate receivedDate;
    private String supplier;
    private BatchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InventoryBatch() {
    }

    public InventoryBatch(Long id, Long facilityId, Long medicineId, String medicineName, String batchNumber, int quantity, int reservedQuantity, int minimumStock, int targetStock, BigDecimal unitPrice, LocalDate manufactureDate, LocalDate expiryDate, LocalDate receivedDate, String supplier, BatchStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.facilityId = facilityId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.minimumStock = minimumStock;
        this.targetStock = targetStock;
        this.unitPrice = unitPrice;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.receivedDate = receivedDate;
        this.supplier = supplier;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    public int getTargetStock() {
        return targetStock;
    }

    public void setTargetStock(int targetStock) {
        this.targetStock = targetStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean isLowStock() {
        return quantity <= minimumStock;
    }

    public boolean isCriticalStock() {
        return quantity <= (minimumStock / 2);
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int days) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(days));
    }

    public long daysUntilExpiry() {
        return expiryDate != null ? ChronoUnit.DAYS.between(LocalDate.now(), expiryDate) : Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "InventoryBatch{" +
                "id=" + id +
                ", facilityId=" + facilityId +
                ", medicineId=" + medicineId +
                ", medicineName='" + medicineName + '\'' +
                ", batchNumber='" + batchNumber + '\'' +
                ", quantity=" + quantity +
                ", reservedQuantity=" + reservedQuantity +
                ", minimumStock=" + minimumStock +
                ", targetStock=" + targetStock +
                ", unitPrice=" + unitPrice +
                ", manufactureDate=" + manufactureDate +
                ", expiryDate=" + expiryDate +
                ", receivedDate=" + receivedDate +
                ", supplier='" + supplier + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
