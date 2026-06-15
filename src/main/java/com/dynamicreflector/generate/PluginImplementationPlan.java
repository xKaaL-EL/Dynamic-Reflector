package com.dynamicreflector.generate;

import com.dynamicreflector.refactor.MethodPlan;

import java.util.Collections;
import java.util.List;

public final class PluginImplementationPlan {
    private final PluginFeatureLayout layout;
    private final List<PluginMethodCopy> methodsToCopy;
    private final List<MethodPlan> skippedMethods;

    public PluginImplementationPlan(
            PluginFeatureLayout layout,
            List<PluginMethodCopy> methodsToCopy,
            List<MethodPlan> skippedMethods
    ) {
        this.layout = layout;
        this.methodsToCopy = Collections.unmodifiableList(methodsToCopy);
        this.skippedMethods = Collections.unmodifiableList(skippedMethods);
    }

    public PluginFeatureLayout getLayout() {
        return layout;
    }

    public List<PluginMethodCopy> getMethodsToCopy() {
        return methodsToCopy;
    }

    public List<MethodPlan> getSkippedMethods() {
        return skippedMethods;
    }
}
