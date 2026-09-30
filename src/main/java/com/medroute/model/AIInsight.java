package com.medroute.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AIInsight {
    public static final String AI_DISCLAIMER = "AI-generated logistics insight. This is not medical advice and should not be used for clinical decision-making.";

    private Long id;
    private Long facilityId;
    private transient String facilityName;
    private Long medicineId;
    private transient String medicineName;
    private InsightType insightType;
    private String title;
    private String content;
    private Severity severity;
    private BigDecimal riskScore;
    private String metadata;
    private String requestHash;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public AIInsight() {
    }

    public AIInsight(Long id, Long facilityId, String facilityName, Long medicineId, String medicineName, InsightType insightType, String title, String content, Severity severity, BigDecimal riskScore, String metadata, boolean read, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.insightType = insightType;
        this.title = title;
        this.content = content;
        this.severity = severity;
        this.riskScore = riskScore;
        this.metadata = metadata;
        this.read = read;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
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

    public InsightType getInsightType() {
        return insightType;
    }

    public void setInsightType(InsightType insightType) {
        this.insightType = insightType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    @Override
    public String toString() {
        return "AIInsight{" +
                "id=" + id +
                ", facilityId=" + facilityId +
                ", facilityName='" + facilityName + '\'' +
                ", medicineId=" + medicineId +
                ", medicineName='" + medicineName + '\'' +
                ", insightType=" + insightType +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", severity=" + severity +
                ", riskScore=" + riskScore +
                ", metadata='" + metadata + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
