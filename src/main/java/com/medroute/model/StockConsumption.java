package com.medroute.model;

import java.time.LocalDateTime;

public class StockConsumption {
    private Long id;
    private Long facilityId;
    private Long medicineId;
    private int quantity;
    private String department;
    private String consumedBy;
    private Long recordedBy;
    private String referenceId;
    private String notes;
    private LocalDateTime consumptionDate;

    public StockConsumption() {
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getConsumedBy() {
        return consumedBy;
    }

    public void setConsumedBy(String consumedBy) {
        this.consumedBy = consumedBy;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getConsumptionDate() {
        return consumptionDate;
    }

    public void setConsumptionDate(LocalDateTime consumptionDate) {
        this.consumptionDate = consumptionDate;
    }
}
