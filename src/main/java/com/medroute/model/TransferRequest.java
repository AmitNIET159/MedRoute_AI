package com.medroute.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TransferRequest {
    private Long id;
    private Long fromFacilityId;
    private transient String fromFacilityName;
    private Long toFacilityId;
    private transient String toFacilityName;
    private Long emergencyRequestId;
    private int requestedQuantity;
    private TransferStatus status;
    private BigDecimal matchScore;
    private BigDecimal distanceKm;
    private String notes;
    private Long requestedBy;
    private transient String requestedByName;
    private Long approvedBy;
    private transient String approvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private transient List<TransferItem> items;
    private transient String medicineName;

    public TransferRequest() {
    }

    public TransferRequest(Long id, Long fromFacilityId, String fromFacilityName, Long toFacilityId, String toFacilityName, Long emergencyRequestId, TransferStatus status, BigDecimal matchScore, BigDecimal distanceKm, String notes, Long requestedBy, String requestedByName, Long approvedBy, String approvedByName, LocalDateTime createdAt, LocalDateTime updatedAt, List<TransferItem> items) {
        this.id = id;
        this.fromFacilityId = fromFacilityId;
        this.fromFacilityName = fromFacilityName;
        this.toFacilityId = toFacilityId;
        this.toFacilityName = toFacilityName;
        this.emergencyRequestId = emergencyRequestId;
        this.status = status;
        this.matchScore = matchScore;
        this.distanceKm = distanceKm;
        this.notes = notes;
        this.requestedBy = requestedBy;
        this.requestedByName = requestedByName;
        this.approvedBy = approvedBy;
        this.approvedByName = approvedByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromFacilityId() {
        return fromFacilityId;
    }

    public void setFromFacilityId(Long fromFacilityId) {
        this.fromFacilityId = fromFacilityId;
    }

    public String getFromFacilityName() {
        return fromFacilityName;
    }

    public void setFromFacilityName(String fromFacilityName) {
        this.fromFacilityName = fromFacilityName;
    }

    public Long getToFacilityId() {
        return toFacilityId;
    }

    public void setToFacilityId(Long toFacilityId) {
        this.toFacilityId = toFacilityId;
    }

    public String getToFacilityName() {
        return toFacilityName;
    }

    public void setToFacilityName(String toFacilityName) {
        this.toFacilityName = toFacilityName;
    }

    public Long getEmergencyRequestId() {
        return emergencyRequestId;
    }

    public void setEmergencyRequestId(Long emergencyRequestId) {
        this.emergencyRequestId = emergencyRequestId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
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

    public List<TransferItem> getItems() {
        return items;
    }

    public void setItems(List<TransferItem> items) {
        this.items = items;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    @Override
    public String toString() {
        return "TransferRequest{" +
                "id=" + id +
                ", fromFacilityId=" + fromFacilityId +
                ", fromFacilityName='" + fromFacilityName + '\'' +
                ", toFacilityId=" + toFacilityId +
                ", toFacilityName='" + toFacilityName + '\'' +
                ", emergencyRequestId=" + emergencyRequestId +
                ", status=" + status +
                ", matchScore=" + matchScore +
                ", distanceKm=" + distanceKm +
                ", notes='" + notes + '\'' +
                ", requestedBy=" + requestedBy +
                ", requestedByName='" + requestedByName + '\'' +
                ", approvedBy=" + approvedBy +
                ", approvedByName='" + approvedByName + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", items=" + items +
                '}';
    }
}
