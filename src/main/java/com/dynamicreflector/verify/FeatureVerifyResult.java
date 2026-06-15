package com.dynamicreflector.verify;

import java.util.Collections;
import java.util.List;

public final class FeatureVerifyResult {
    private final boolean success;
    private final List<FeatureVerifyCheck> checks;

    public FeatureVerifyResult(boolean success, List<FeatureVerifyCheck> checks) {
        this.success = success;
        this.checks = Collections.unmodifiableList(checks);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<FeatureVerifyCheck> getChecks() {
        return checks;
    }

    public record FeatureVerifyCheck(String label, boolean success, String detail) {
    }
}
