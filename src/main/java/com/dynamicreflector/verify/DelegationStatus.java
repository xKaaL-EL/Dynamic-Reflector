package com.dynamicreflector.verify;

public enum DelegationStatus {
    CONVERTED("converted"),
    NOT_CONVERTED("not converted"),
    PARTIAL("partial"),
    CONFLICT("conflict");

    private final String label;

    DelegationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
