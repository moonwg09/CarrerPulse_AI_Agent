package com.careerpulse.analysis.entity;

public enum Judgment {
    EXPERIENCED,
    PARTIAL,
    INSUFFICIENT,
    NO_EXPERIENCE,
    MET,
    UNMET,
    UNKNOWN;

    public boolean isMatched() {
        return this == EXPERIENCED || this == MET;
    }

    public boolean isComparable() {
        return this != INSUFFICIENT && this != UNKNOWN;
    }
}
