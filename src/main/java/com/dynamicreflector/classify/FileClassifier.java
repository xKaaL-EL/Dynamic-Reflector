package com.dynamicreflector.classify;

import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.MethodInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FileClassifier {
    public ClassClassification classify(JavaClassInfo info) {
        List<String> reasons = new ArrayList<>();

        if (isGeneratedOrResourceFile(info.getFilePath())) {
            reasons.add("generated/build/resource file");
            return new ClassClassification(ClassificationBucket.HARD_EXCLUDED, reasons);
        }

        if (isGeneratedProtectionFrameworkClass(info)) {
            reasons.add("generated Dynamic Reflector protection framework class");
            return new ClassClassification(ClassificationBucket.HARD_EXCLUDED, reasons);
        }

        if (isConstantsOnly(info)) {
            reasons.add("constants-only class");
            return new ClassClassification(ClassificationBucket.HARD_EXCLUDED, reasons);
        }

        if (isModelOnly(info)) {
            reasons.add("model/DTO-style class with no meaningful logic");
            return new ClassClassification(ClassificationBucket.HARD_EXCLUDED, reasons);
        }

        if (isLifecycleClass(info)) {
            reasons.add("Android lifecycle/component class; keep in base app and extract/wrap inner logic only");
            return new ClassClassification(ClassificationBucket.REVIEW_REQUIRED, reasons);
        }

        if (usesAndroidRuntimeTypes(info)) {
            reasons.add("uses Android runtime types such as Context/Intent; review before wrapping");
            return new ClassClassification(ClassificationBucket.REVIEW_REQUIRED, reasons);
        }

        if (hasMeaningfulMethods(info)) {
            reasons.add("contains executable helper/business-style methods");
            return new ClassClassification(ClassificationBucket.CANDIDATE, reasons);
        }

        reasons.add("no executable methods found");
        return new ClassClassification(ClassificationBucket.HARD_EXCLUDED, reasons);
    }

    private boolean isGeneratedOrResourceFile(Path path) {
        if (path == null) {
            return false;
        }
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = path.getFileName().toString();
        return normalized.contains("/build/")
                || normalized.contains("/.gradle/")
                || normalized.contains("/generated/")
                || normalized.contains("/res/")
                || fileName.equals("R.java")
                || fileName.equals("BuildConfig.java");
    }

    private boolean isGeneratedProtectionFrameworkClass(JavaClassInfo info) {
        String qualifiedName = info.getQualifiedName();
        if (qualifiedName == null || !qualifiedName.contains(".protection.")) {
            return false;
        }
        String simpleName = info.getSimpleName();
        return simpleName.equals("AndroidDexPluginLoader")
                || simpleName.equals("PluginException")
                || simpleName.equals("PluginRegistry")
                || simpleName.equals("PremiumFeatureManager")
                || simpleName.equals("PluginConfig");
    }

    private boolean isConstantsOnly(JavaClassInfo info) {
        return info.getFieldCount() > 0
                && info.getFieldCount() == info.getStaticFinalFieldCount()
                && info.getMethods().stream().noneMatch(MethodInfo::hasBody);
    }

    private boolean isModelOnly(JavaClassInfo info) {
        if (info.getFieldCount() == 0 || info.getMethods().isEmpty()) {
            return false;
        }
        return info.getMethods().stream().allMatch(this::isModelMethod);
    }

    private boolean isModelMethod(MethodInfo method) {
        String name = method.getName();
        return name.startsWith("get")
                || name.startsWith("set")
                || name.startsWith("is")
                || name.equals("equals")
                || name.equals("hashCode")
                || name.equals("toString");
    }

    private boolean isLifecycleClass(JavaClassInfo info) {
        String combined = (info.getSuperClassName() + " " + String.join(" ", info.getInterfaceNames())).toLowerCase(Locale.ROOT);
        return combined.contains("activity")
                || combined.contains("service")
                || combined.contains("broadcastreceiver")
                || combined.contains("notificationlistenerservice")
                || combined.contains("worker")
                || combined.contains("fragment")
                || combined.contains("application")
                || combined.contains("contentprovider");
    }

    private boolean usesAndroidRuntimeTypes(JavaClassInfo info) {
        for (MethodInfo method : info.getMethods()) {
            for (String parameterType : method.getParameterTypes()) {
                String lower = parameterType.toLowerCase(Locale.ROOT);
                if (lower.contains("android.content.context")
                        || lower.contains("android.content.intent")
                        || lower.contains("android.app.pendingintent")
                        || lower.contains("android.app.notification")) {
                    return true;
                }
            }
            String returnType = method.getReturnType().toLowerCase(Locale.ROOT);
            if (returnType.startsWith("android.")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMeaningfulMethods(JavaClassInfo info) {
        return info.getMethods().stream().anyMatch(method -> method.hasBody() && !method.isPrivateMethod());
    }
}
