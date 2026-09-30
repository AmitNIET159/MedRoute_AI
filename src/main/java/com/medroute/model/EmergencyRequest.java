package com.medroute.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmergencyRequest {
    private Long id;
    private Long requestingFacilityId;
    private transient String facilityName;
    private Long medicineId;
    private transient String medicineName;
    private int quantityNeeded;
    private int quantityFulfilled;
    private Urgency urgency;
    private LocalDate requiredByDate;
    private String reason;
    private RequestStatus status;
    private BigDecimal aiPriorityScore;
    private Long createdBy;
    private transient String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyRequest() {
    }

    public EmergencyRequest(Long id, Long requestingFacilityId, String facilityName, Long medicineId, String medicineName, int quantityNeeded, int quantityFulfilled, Urgency urgency, LocalDate requiredByDate, String reason, RequestStatus status, BigDecimal aiPriorityScore, Long createdBy, String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.requestingFacilityId = requestingFacilityId;
        this.facilityName = facilityName;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantityNeeded = quantityNeeded;
        this.quantityFulfilled = quantityFulfilled;
        this.urgency = urgency;
        this.requiredByDate = requiredByDate;
        this.reason = reason;
        this.status = status;
        this.aiPriorityScore = aiPriorityScore;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestingFacilityId() {
        return requestingFacilityId;
    }

    public void setRequestingFacilityId(Long requestingFacilityId) {
        this.requestingFacilityId = requestingFacilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
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

    public int getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(int quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
    }

    public int getQuantityFulfilled() {
        return quantityFulfilled;
    }

    public void setQuantityFulfilled(int quantityFulfilled) {
        this.quantityFulfilled = quantityFulfilled;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public LocalDate getRequiredByDate() {
        return requiredByDate;
    }

    public void setRequiredByDate(LocalDate requiredByDate) {
        this.requiredByDate = requiredByDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public BigDecimal getAiPriorityScore() {
        return aiPriorityScore;
    }

    public void setAiPriorityScore(BigDecimal aiPriorityScore) {
        this.aiPriorityScore = aiPriorityScore;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    public int getRemainingQuantity() {
        return quantityNeeded - quantityFulfilled;
    }

    public double getFulfillmentPercentage() {
        return quantityNeeded > 0 ? (quantityFulfilled * 100.0 / quantityNeeded) : 0;
    }

    public boolean isOverdue() {
        return requiredByDate != null && requiredByDate.isBefore(LocalDate.now()) && status == RequestStatus.OPEN;
    }

    @Override
    public String toString() {
        return "EmergencyRequest{" +
                "id=" + id +
                ", requestingFacilityId=" + requestingFacilityId +
                ", facilityName='" + facilityName + '\'' +
                ", medicineId=" + medicineId +
                ", medicineName='" + medicineName + '\'' +
                ", quantityNeeded=" + quantityNeeded +
                ", quantityFulfilled=" + quantityFulfilled +
                ", urgency=" + urgency +
                ", requiredByDate=" + requiredByDate +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", aiPriorityScore=" + aiPriorityScore +
                ", createdBy=" + createdBy +
                ", createdByName='" + createdByName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
