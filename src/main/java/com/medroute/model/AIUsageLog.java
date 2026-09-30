package com.medroute.model;

import java.time.LocalDateTime;

public class AIUsageLog {
    private Long id;
    private Long userId; // Nullable for system calls
    private String requestType;
    private String modelUsed;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer responseTimeMs;
    private Boolean isCached;
    private String status; // SUCCESS, FAILED, TIMEOUT, FALLBACK
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public Boolean getIsCached() { return isCached; }
    public void setIsCached(Boolean isCached) { this.isCached = isCached; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
