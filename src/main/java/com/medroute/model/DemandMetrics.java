package com.medroute.model;

public class DemandMetrics {
    private Long facilityId;
    private Long medicineId;
    private String medicineName;
    private int currentStock;
    private int reservedStock;
    private int availableStock;
    private Double averageDailyConsumption;
    private Double daysOfStock;
    private Double trendPercent;
    private String trendClassification;
    private String expiryRiskLevel;
    private Integer nearestExpiryDays;
    private String dataStatus;

    // Getters and Setters

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public Double getAverageDailyConsumption() { return averageDailyConsumption; }
    public void setAverageDailyConsumption(Double averageDailyConsumption) { this.averageDailyConsumption = averageDailyConsumption; }

    public Double getDaysOfStock() { return daysOfStock; }
    public void setDaysOfStock(Double daysOfStock) { this.daysOfStock = daysOfStock; }

    public Double getTrendPercent() { return trendPercent; }
    public void setTrendPercent(Double trendPercent) { this.trendPercent = trendPercent; }

    public String getTrendClassification() { return trendClassification; }
    public void setTrendClassification(String trendClassification) { this.trendClassification = trendClassification; }

    public String getExpiryRiskLevel() { return expiryRiskLevel; }
    public void setExpiryRiskLevel(String expiryRiskLevel) { this.expiryRiskLevel = expiryRiskLevel; }

    public Integer getNearestExpiryDays() { return nearestExpiryDays; }
    public void setNearestExpiryDays(Integer nearestExpiryDays) { this.nearestExpiryDays = nearestExpiryDays; }

    public String getDataStatus() { return dataStatus; }
    public void setDataStatus(String dataStatus) { this.dataStatus = dataStatus; }
}
