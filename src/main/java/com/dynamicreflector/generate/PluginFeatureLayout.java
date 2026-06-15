package com.dynamicreflector.generate;

import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.util.JavaNameUtils;

import java.nio.file.Path;

public final class PluginFeatureLayout {
    private final RefactorPlan refactorPlan;
    private final String implementationPackage;
    private final String implementationName;
    private final String implementationQualifiedName;
    private final Path implementationPath;
    private final Path pluginConfigPath;
    private final String allowedImplementationPrefix;
    private final String apiQualifiedName;
    private final String featureName;

    private PluginFeatureLayout(
            RefactorPlan refactorPlan,
            String implementationPackage,
            String implementationName,
            String implementationQualifiedName,
            Path implementationPath,
            Path pluginConfigPath,
            String allowedImplementationPrefix,
            String apiQualifiedName,
            String featureName
    ) {
        this.refactorPlan = refactorPlan;
        this.implementationPackage = implementationPackage;
        this.implementationName = implementationName;
        this.implementationQualifiedName = implementationQualifiedName;
        this.implementationPath = implementationPath;
        this.pluginConfigPath = pluginConfigPath;
        this.allowedImplementationPrefix = allowedImplementationPrefix;
        this.apiQualifiedName = apiQualifiedName;
        this.featureName = featureName;
    }

    public static PluginFeatureLayout from(RefactorPlan plan) {
        String implementationPackage = plan.getProject().getBasePackage() + ".premium.impl";
        String implementationName = plan.getClassInfo().getSimpleName() + "Impl";
        String implementationQualifiedName = implementationPackage + "." + implementationName;
        Path implementationPath = plan.getProject()
                .getProjectRoot()
                .resolve("premium-plugin")
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(JavaNameUtils.packageToPath(implementationPackage))
                .resolve(implementationName + ".java");
        Path javaRoot = plan.getProject().getJavaSourceRoots().get(0);
        Path pluginConfigPath = javaRoot.resolve(JavaNameUtils.packageToPath(plan.getProject().getBasePackage()))
                .resolve("protection")
                .resolve("config")
                .resolve("PluginConfig.java");
        return new PluginFeatureLayout(
                plan,
                implementationPackage,
                implementationName,
                implementationQualifiedName,
                implementationPath,
                pluginConfigPath,
                implementationPackage,
                plan.getApiPackage() + "." + plan.getApiName(),
                featureName(plan.getClassInfo().getSimpleName())
        );
    }

    public RefactorPlan getRefactorPlan() {
        return refactorPlan;
    }

    public String getImplementationPackage() {
        return implementationPackage;
    }

    public String getImplementationName() {
        return implementationName;
    }

    public String getImplementationQualifiedName() {
        return implementationQualifiedName;
    }

    public Path getImplementationPath() {
        return implementationPath;
    }

    public Path getPluginConfigPath() {
        return pluginConfigPath;
    }

    public String getAllowedImplementationPrefix() {
        return allowedImplementationPrefix;
    }

    public String getApiQualifiedName() {
        return apiQualifiedName;
    }

    public String getFeatureName() {
        return featureName;
    }

    private static String featureName(String simpleName) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char value = simpleName.charAt(i);
            if (Character.isUpperCase(value) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(value));
        }
        return builder.toString();
    }
}
