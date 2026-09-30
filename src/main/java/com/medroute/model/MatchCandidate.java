package com.medroute.model;

import java.math.BigDecimal;

/**
 * Represents a scored donor candidate for an emergency request.
 * All scoring is deterministic — no AI dependency.
 */
public class MatchCandidate {

    private Long facilityId;
    private String facilityName;
    private FacilityType facilityType;
    private String city;
    private double distanceKm;

    // Inventory
    private int totalQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int transferableQuantity;

    // Demand context
    private Double averageDailyConsumption;
    private Double daysOfStock;
    private String donorRiskLevel;
    private int nearestExpiryDays;

    // Reliability
    private BigDecimal facilityReliabilityScore;

    // Component scores (0–100)
    private int distanceScore;
    private int surplusScore;
    private int donorRiskScoreValue;
    private int expiryScore;
    private int reliabilityScoreValue;

    // Final composite
    private int matchScore;
    private String matchStatus;

    public MatchCandidate() {
    }

    // --- Getters and setters ---

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public FacilityType getFacilityType() { return facilityType; }
    public void setFacilityType(FacilityType facilityType) { this.facilityType = facilityType; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public int getTransferableQuantity() { return transferableQuantity; }
    public void setTransferableQuantity(int transferableQuantity) { this.transferableQuantity = transferableQuantity; }

    public Double getAverageDailyConsumption() { return averageDailyConsumption; }
    public void setAverageDailyConsumption(Double averageDailyConsumption) { this.averageDailyConsumption = averageDailyConsumption; }

    public Double getDaysOfStock() { return daysOfStock; }
    public void setDaysOfStock(Double daysOfStock) { this.daysOfStock = daysOfStock; }

    public String getDonorRiskLevel() { return donorRiskLevel; }
    public void setDonorRiskLevel(String donorRiskLevel) { this.donorRiskLevel = donorRiskLevel; }

    public int getNearestExpiryDays() { return nearestExpiryDays; }
    public void setNearestExpiryDays(int nearestExpiryDays) { this.nearestExpiryDays = nearestExpiryDays; }

    public BigDecimal getFacilityReliabilityScore() { return facilityReliabilityScore; }
    public void setFacilityReliabilityScore(BigDecimal facilityReliabilityScore) { this.facilityReliabilityScore = facilityReliabilityScore; }

    public int getDistanceScore() { return distanceScore; }
    public void setDistanceScore(int distanceScore) { this.distanceScore = distanceScore; }

    public int getSurplusScore() { return surplusScore; }
    public void setSurplusScore(int surplusScore) { this.surplusScore = surplusScore; }

    public int getDonorRiskScoreValue() { return donorRiskScoreValue; }
    public void setDonorRiskScoreValue(int donorRiskScoreValue) { this.donorRiskScoreValue = donorRiskScoreValue; }

    public int getExpiryScore() { return expiryScore; }
    public void setExpiryScore(int expiryScore) { this.expiryScore = expiryScore; }

    public int getReliabilityScoreValue() { return reliabilityScoreValue; }
    public void setReliabilityScoreValue(int reliabilityScoreValue) { this.reliabilityScoreValue = reliabilityScoreValue; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    @Override
    public String toString() {
        return "MatchCandidate{" +
                "facilityName='" + facilityName + '\'' +
                ", distanceKm=" + String.format("%.1f", distanceKm) +
                ", transferable=" + transferableQuantity +
                ", risk=" + donorRiskLevel +
                ", score=" + matchScore +
                ", status=" + matchStatus +
                '}';
    }
}
