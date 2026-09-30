package com.medroute.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private Long userId;
    private Long facilityId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private String referenceId;
    private String referenceType;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long id, Long userId, Long facilityId, NotificationType type, String title, String message, boolean read, String referenceId, String referenceType, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.facilityId = facilityId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", facilityId=" + facilityId +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", read=" + read +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
