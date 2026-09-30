package com.medroute.model;

import java.time.LocalDateTime;

public class DemandAnalysis {
    private DemandMetrics metrics;
    private RiskScore riskScore;
    private LocalDateTime calculatedAt;

    public DemandMetrics getMetrics() { return metrics; }
    public void setMetrics(DemandMetrics metrics) { this.metrics = metrics; }

    public RiskScore getRiskScore() { return riskScore; }
    public void setRiskScore(RiskScore riskScore) { this.riskScore = riskScore; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
