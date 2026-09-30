package com.medroute.model;

import java.time.LocalDateTime;

public class Medicine {
    private Long id;
    private String name;
    private String genericName;
    private Long categoryId;
    private transient String categoryName;
    private String unit;
    private String description;
    private boolean requiresColdChain;
    private boolean controlled;
    private LocalDateTime createdAt;

    public Medicine() {
    }

    public Medicine(Long id, String name, String genericName, Long categoryId, String categoryName, String unit, String description, boolean requiresColdChain, boolean controlled, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unit = unit;
        this.description = description;
        this.requiresColdChain = requiresColdChain;
        this.controlled = controlled;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequiresColdChain() {
        return requiresColdChain;
    }

    public void setRequiresColdChain(boolean requiresColdChain) {
        this.requiresColdChain = requiresColdChain;
    }

    public boolean isControlled() {
        return controlled;
    }

    public void setControlled(boolean controlled) {
        this.controlled = controlled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Medicine{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", genericName='" + genericName + '\'' +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", unit='" + unit + '\'' +
                ", description='" + description + '\'' +
                ", requiresColdChain=" + requiresColdChain +
                ", controlled=" + controlled +
                ", createdAt=" + createdAt +
                '}';
    }
}
