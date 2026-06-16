package com.dynamicreflector.convert;

import java.util.Collections;
import java.util.List;

public final class ConversionPreconditionResult {
    private final List<ConversionPrecondition> checks;

    public ConversionPreconditionResult(List<ConversionPrecondition> checks) {
        this.checks = Collections.unmodifiableList(checks);
    }

    public List<ConversionPrecondition> getChecks() {
        return checks;
    }

    public boolean isSuccess() {
        return checks.stream().allMatch(ConversionPrecondition::isSuccess);
    }

    public ConversionPrecondition firstFailure() {
        return checks.stream().filter(check -> !check.isSuccess()).findFirst().orElse(null);
    }
}
