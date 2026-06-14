package com.dynamicreflector.refactor;

import com.dynamicreflector.spoon.MethodInfo;

public final class MethodPlan {
    private final MethodInfo method;
    private final boolean included;
    private final String reason;

    public MethodPlan(MethodInfo method, boolean included, String reason) {
        this.method = method;
        this.included = included;
        this.reason = reason;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public boolean isIncluded() {
        return included;
    }

    public String getReason() {
        return reason;
    }
}
