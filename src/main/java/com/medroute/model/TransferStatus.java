package com.medroute.model;

public enum TransferStatus {
    REQUESTED,
    OFFERED,
    ACCEPTED,
    SCHEDULED,
    IN_TRANSIT,
    RECEIVED,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public boolean isActive() {
        return this == REQUESTED || this == OFFERED || this == ACCEPTED || 
               this == SCHEDULED || this == IN_TRANSIT || this == RECEIVED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || 
               this == CANCELLED || this == EXPIRED;
    }
}
