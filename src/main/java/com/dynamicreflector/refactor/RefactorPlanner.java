package com.dynamicreflector.refactor;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.classify.ClassificationBucket;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.util.JavaNameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RefactorPlanner {
    private final FileClassifier classifier = new FileClassifier();

    public RefactorPlan plan(AndroidProject project, JavaClassInfo classInfo) throws RefactorValidationException {
        ClassClassification classification = classifier.classify(classInfo);
        validateClass(classInfo, classification);

        List<MethodPlan> methods = new ArrayList<>();
        boolean unsupportedPublicInstanceMethod = false;
        for (MethodInfo method : classInfo.getMethods()) {
            MethodPlan methodPlan = planMethod(method);
            methods.add(methodPlan);
            if (!methodPlan.isIncluded() && isPublicInstanceMethod(method) && isUnsupportedSignatureReason(methodPlan.getReason())) {
                unsupportedPublicInstanceMethod = true;
            }
        }

        if (unsupportedPublicInstanceMethod) {
            throw new RefactorValidationException(
                    "Class contains public instance methods with unsupported signatures. "
                            + skippedMethodSummary(methods)
            );
        }

        boolean hasIncluded = methods.stream().anyMatch(MethodPlan::isIncluded);
        if (!hasIncluded) {
            throw new RefactorValidationException(
                    "No supported public instance methods found. No files were changed. "
                            + skippedMethodSummary(methods)
            );
        }

        String apiPackage = project.getBasePackage() + ".protection.api";
        String wrapperPackage = project.getBasePackage() + ".protection.wrapper";
        String apiName = classInfo.getSimpleName() + "Api";
        String wrapperName = classInfo.getSimpleName() + "Wrapper";

        Path javaRoot = project.getJavaSourceRoots().get(0);
        Path apiPath = javaRoot.resolve(JavaNameUtils.packageToPath(apiPackage)).resolve(apiName + ".java");
        Path wrapperPath = javaRoot.resolve(JavaNameUtils.packageToPath(wrapperPackage)).resolve(wrapperName + ".java");

        return new RefactorPlan(
                project,
                classInfo,
                classification,
                apiPackage,
                wrapperPackage,
                apiName,
                wrapperName,
                apiPath,
                wrapperPath,
                methods
        );
    }

    private void validateClass(JavaClassInfo classInfo, ClassClassification classification) throws RefactorValidationException {
        if (classification.getBucket() != ClassificationBucket.CANDIDATE) {
            throw new RefactorValidationException(
                    "Class is not a safe Batch 2 candidate: " + classification.getBucket()
                            + " (" + String.join("; ", classification.getReasons()) + ")"
            );
        }
        if (classInfo.isInterfaceType()) {
            throw new RefactorValidationException("Interfaces are not supported for Batch 2 refactor preparation.");
        }
        if (classInfo.isAbstractType()) {
            throw new RefactorValidationException("Abstract classes are not supported for Batch 2 refactor preparation.");
        }
        if (classInfo.isEnumType()) {
            throw new RefactorValidationException("Enums are not supported for Batch 2 refactor preparation.");
        }
        if (classInfo.isAnnotationType()) {
            throw new RefactorValidationException("Annotations are not supported for Batch 2 refactor preparation.");
        }
    }

    private MethodPlan planMethod(MethodInfo method) {
        if (method.isPrivateMethod()) {
            return skipped(method, "private methods are skipped in Batch 2");
        }
        if (method.isProtectedMethod()) {
            return skipped(method, "protected methods are skipped in Batch 2");
        }
        if (!method.isPublicMethod()) {
            return skipped(method, "package-private methods are skipped in Batch 2");
        }
        if (method.isStaticMethod()) {
            return skipped(method, "static methods are skipped in Batch 2");
        }
        if (!method.hasBody()) {
            return skipped(method, "methods without bodies are unsupported");
        }
        if (method.isAbstractMethod()) {
            return skipped(method, "abstract methods are unsupported");
        }
        if (method.isNativeMethod()) {
            return skipped(method, "native methods are unsupported");
        }
        if (method.isVarArgs()) {
            return skipped(method, "varargs methods are unsupported in Batch 2");
        }
        if (method.getTypeParameterCount() > 0) {
            return skipped(method, "generic method type parameters are unsupported in Batch 2");
        }
        if (!isSupportedType(method.getReturnType())) {
            return skipped(method, "unsupported return type: " + method.getReturnType());
        }
        for (String parameterType : method.getParameterTypes()) {
            if (!isSupportedType(parameterType)) {
                return skipped(method, "unsupported parameter type: " + parameterType);
            }
        }
        for (String thrownType : method.getThrownTypes()) {
            if (!isSupportedType(thrownType)) {
                return skipped(method, "unsupported thrown type: " + thrownType);
            }
        }
        return new MethodPlan(method, true, "included");
    }

    private boolean isPublicInstanceMethod(MethodInfo method) {
        return method.isPublicMethod() && !method.isStaticMethod();
    }

    private boolean isUnsupportedSignatureReason(String reason) {
        return reason.startsWith("methods without bodies")
                || reason.startsWith("abstract methods")
                || reason.startsWith("native methods")
                || reason.startsWith("varargs")
                || reason.startsWith("generic method")
                || reason.startsWith("unsupported");
    }

    private boolean isSupportedType(String typeName) {
        if (typeName == null || typeName.isBlank() || typeName.equals("unknown")) {
            return false;
        }
        if (typeName.contains("<") || typeName.contains(">") || typeName.contains("?") || typeName.contains("$")) {
            return false;
        }
        String normalized = typeName;
        while (normalized.endsWith("[]")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized.matches("void|boolean|byte|char|short|int|long|float|double")
                || normalized.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    }

    private MethodPlan skipped(MethodInfo method, String reason) {
        return new MethodPlan(method, false, reason);
    }

    private String skippedMethodSummary(List<MethodPlan> methods) {
        List<String> skipped = new ArrayList<>();
        for (MethodPlan methodPlan : methods) {
            if (!methodPlan.isIncluded()) {
                skipped.add(methodPlan.getMethod().getName() + ": " + methodPlan.getReason());
            }
        }
        if (skipped.isEmpty()) {
            return "";
        }
        return "Skipped methods: " + String.join("; ", skipped);
    }
}
