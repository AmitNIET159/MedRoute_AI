package com.medroute.model;

public enum Urgency {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int priorityWeight;

    Urgency(int priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }
}
