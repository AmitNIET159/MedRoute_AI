package com.medroute.model;

import java.time.LocalDateTime;

public class TransferItem {
    private Long id;
    private Long transferId;
    private Long batchId;
    private Long medicineId;
    private transient String medicineName;
    private transient String batchNumber;
    private int quantity;
    private LocalDateTime createdAt;

    public TransferItem() {
    }

    public TransferItem(Long id, Long transferId, Long batchId, Long medicineId, String medicineName, String batchNumber, int quantity, LocalDateTime createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.batchId = batchId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TransferItem{" +
                "id=" + id +
                ", transferId=" + transferId +
                ", batchId=" + batchId +
                ", medicineId=" + medicineId +
                ", medicineName='" + medicineName + '\'' +
                ", batchNumber='" + batchNumber + '\'' +
                ", quantity=" + quantity +
                ", createdAt=" + createdAt +
                '}';
    }
}
