package com.dynamicreflector.verify;

import com.dynamicreflector.classify.ClassificationBucket;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.generate.PluginConfigUpdater;
import com.dynamicreflector.generate.PluginFeatureLayout;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonClassInspector;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.verify.FeatureVerifyResult.FeatureVerifyCheck;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class FeatureVerifier {
    public FeatureVerifyResult verify(AndroidProject project, SpoonModelContext model, RefactorPlan plan)
            throws IOException {
        PluginFeatureLayout layout = PluginFeatureLayout.from(plan);
        PluginConfigUpdater configUpdater = new PluginConfigUpdater();
        List<FeatureVerifyCheck> checks = new ArrayList<>();

        checks.add(fileCheck("API", Files.exists(plan.getApiPath())));
        checks.add(fileCheck("Wrapper", Files.exists(plan.getWrapperPath())));
        checks.add(fileCheck("Plugin implementation", Files.exists(layout.getImplementationPath())));
        checks.add(new FeatureVerifyCheck(
                "PluginConfig mapping",
                configUpdater.containsMapping(layout.getPluginConfigPath(), layout),
                layout.getRefactorPlan().getApiName() + ".class -> \"" + layout.getImplementationQualifiedName() + "\""
        ));
        checks.add(new FeatureVerifyCheck(
                "Allowed implementation prefix",
                configUpdater.containsAllowedPrefix(layout.getPluginConfigPath(), layout.getAllowedImplementationPrefix()),
                layout.getAllowedImplementationPrefix()
        ));
        checks.add(new FeatureVerifyCheck(
                "Protection candidate exclusion",
                protectionClassesExcluded(model),
                "protection.* generated classes remain excluded"
        ));

        boolean success = checks.stream().allMatch(FeatureVerifyCheck::success);
        return new FeatureVerifyResult(success, checks);
    }

    private FeatureVerifyCheck fileCheck(String label, boolean exists) {
        return new FeatureVerifyCheck(label, exists, exists ? "found" : "missing");
    }

    private boolean protectionClassesExcluded(SpoonModelContext model) {
        FileClassifier classifier = new FileClassifier();
        for (JavaClassInfo info : new SpoonClassInspector().listClasses(model)) {
            if (info.getQualifiedName().contains(".protection.")
                    && classifier.classify(info).getBucket() != ClassificationBucket.HARD_EXCLUDED) {
                return false;
            }
        }
        return true;
    }
}
