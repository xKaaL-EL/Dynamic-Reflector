package com.dynamicreflector.classify;

import java.util.Collections;
import java.util.List;

public final class ClassClassification {
    private final ClassificationBucket bucket;
    private final List<String> reasons;

    public ClassClassification(ClassificationBucket bucket, List<String> reasons) {
        this.bucket = bucket;
        this.reasons = Collections.unmodifiableList(reasons);
    }

    public ClassificationBucket getBucket() {
        return bucket;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public boolean isWrapperPossible() {
        return bucket == ClassificationBucket.CANDIDATE;
    }
}
