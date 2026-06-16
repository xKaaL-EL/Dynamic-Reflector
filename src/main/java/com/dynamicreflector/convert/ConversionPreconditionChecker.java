package com.dynamicreflector.convert;

import com.dynamicreflector.generate.PluginConfigUpdater;
import com.dynamicreflector.generate.PluginFeatureLayout;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.verify.FrameworkVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ConversionPreconditionChecker {
    public ConversionPreconditionResult check(RefactorPlan plan) throws IOException {
        PluginFeatureLayout layout = PluginFeatureLayout.from(plan);
        PluginConfigUpdater configUpdater = new PluginConfigUpdater();
        List<ConversionPrecondition> checks = new ArrayList<>();

        String initCommand = "dynamic-reflector --init " + quoteIfNeeded(plan.getProject().getProjectRoot().toString());
        String refactorCommand = "dynamic-reflector --refactor "
                + quoteIfNeeded(plan.getProject().getProjectRoot().toString())
                + " " + plan.getClassInfo().getSimpleName() + " --apply";
        String generatePluginCommand = "dynamic-reflector --generate-plugin "
                + quoteIfNeeded(plan.getProject().getProjectRoot().toString())
                + " " + plan.getClassInfo().getSimpleName() + " --apply";

        checks.add(new ConversionPrecondition(
                "Runtime framework",
                new FrameworkVerifier().verify(plan.getProject()).isSuccess(),
                initCommand
        ));
        checks.add(new ConversionPrecondition(
                "API",
                Files.exists(plan.getApiPath()),
                refactorCommand
        ));
        checks.add(new ConversionPrecondition(
                "Wrapper",
                Files.exists(plan.getWrapperPath()),
                refactorCommand
        ));
        checks.add(new ConversionPrecondition(
                "Plugin implementation",
                Files.exists(layout.getImplementationPath()),
                generatePluginCommand
        ));
        checks.add(new ConversionPrecondition(
                "PluginConfig mapping",
                configUpdater.containsMapping(layout.getPluginConfigPath(), layout),
                generatePluginCommand
        ));
        checks.add(new ConversionPrecondition(
                "Allowed implementation prefix",
                configUpdater.containsAllowedPrefix(layout.getPluginConfigPath(), layout.getAllowedImplementationPrefix()),
                generatePluginCommand
        ));

        return new ConversionPreconditionResult(checks);
    }

    private String quoteIfNeeded(String value) {
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }
}
