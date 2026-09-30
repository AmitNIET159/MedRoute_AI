package com.medroute.model;

public class RiskScore {
    private int coverageScore;
    private int trendScore;
    private int emergencyScore;
    private int expiryScore;
    private int priorityScore;
    private int finalScore;
    private String riskLevel;

    public int getCoverageScore() { return coverageScore; }
    public void setCoverageScore(int coverageScore) { this.coverageScore = coverageScore; }

    public int getTrendScore() { return trendScore; }
    public void setTrendScore(int trendScore) { this.trendScore = trendScore; }

    public int getEmergencyScore() { return emergencyScore; }
    public void setEmergencyScore(int emergencyScore) { this.emergencyScore = emergencyScore; }

    public int getExpiryScore() { return expiryScore; }
    public void setExpiryScore(int expiryScore) { this.expiryScore = expiryScore; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}
