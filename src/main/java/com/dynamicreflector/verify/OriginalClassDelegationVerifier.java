package com.dynamicreflector.verify;

import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public final class OriginalClassDelegationVerifier {
    public DelegationStatus status(SpoonModelContext model, RefactorPlan plan) throws IOException {
        if (plan.getIncludedMethods().isEmpty() || plan.getClassInfo().getFilePath() == null) {
            return DelegationStatus.NOT_CONVERTED;
        }
        String content = Files.readString(plan.getClassInfo().getFilePath(), StandardCharsets.UTF_8);
        CtType<?> type = findType(model, plan.getClassInfo().getQualifiedName());
        int converted = 0;
        int notConverted = 0;
        int conflicts = 0;

        for (MethodPlan methodPlan : plan.getIncludedMethods()) {
            MethodInfo methodInfo = methodPlan.getMethod();
            CtMethod<?> method = findMethod(type, methodInfo);
            SourcePosition bodyPosition = method.getBody() == null ? null : method.getBody().getPosition();
            if (bodyPosition == null || !bodyPosition.isValidPosition()) {
                conflicts++;
                continue;
            }
            String originalBody = content.substring(bodyPosition.getSourceStart(), bodyPosition.getSourceEnd() + 1);
            String expectedBody = replacementBody(content, bodyPosition.getSourceStart(), methodInfo, plan);
            if (sameCode(originalBody, expectedBody)) {
                converted++;
            } else if (originalBody.contains("PremiumFeatureManager") || originalBody.contains(plan.getApiName() + ".class")) {
                conflicts++;
            } else {
                notConverted++;
            }
        }

        if (conflicts > 0) {
            return DelegationStatus.CONFLICT;
        }
        if (converted == plan.getIncludedMethods().size()) {
            return DelegationStatus.CONVERTED;
        }
        if (converted > 0 && notConverted > 0) {
            return DelegationStatus.PARTIAL;
        }
        return DelegationStatus.NOT_CONVERTED;
    }

    private CtType<?> findType(SpoonModelContext model, String qualifiedName) {
        return model.getModel()
                .getAllTypes()
                .stream()
                .filter(type -> qualifiedName.equals(type.getQualifiedName()))
                .findFirst()
                .orElseThrow();
    }

    private CtMethod<?> findMethod(CtType<?> type, MethodInfo methodInfo) {
        return type.getMethods()
                .stream()
                .filter(method -> method.getSimpleName().equals(methodInfo.getName()))
                .filter(method -> method.getParameters().size() == methodInfo.getParameterTypes().size())
                .filter(method -> parameterTypes(method).equals(methodInfo.getParameterTypes()))
                .findFirst()
                .orElseThrow();
    }

    private List<String> parameterTypes(CtMethod<?> method) {
        return method.getParameters()
                .stream()
                .map(parameter -> parameter.getType() == null ? "unknown" : parameter.getType().getQualifiedName())
                .collect(Collectors.toList());
    }

    private String replacementBody(String source, int bodyStart, MethodInfo method, RefactorPlan plan) {
        String lineSeparator = source.contains("\r\n") ? "\r\n" : "\n";
        String methodIndent = methodIndent(source, bodyStart);
        String innerIndent = methodIndent + "    ";
        String call = "PremiumFeatureManager.get(" + plan.getApiName() + ".class)."
                + method.getName() + "(" + String.join(", ", method.getParameterNames()) + ")";
        String statement = method.getReturnType().equals("void") ? call + ";" : "return " + call + ";";
        return "{" + lineSeparator + innerIndent + statement + lineSeparator + methodIndent + "}";
    }

    private String methodIndent(String source, int bodyStart) {
        int lineStart = source.lastIndexOf('\n', bodyStart);
        lineStart = lineStart < 0 ? 0 : lineStart + 1;
        int firstNonWhitespace = lineStart;
        while (firstNonWhitespace < source.length()) {
            char value = source.charAt(firstNonWhitespace);
            if (value != ' ' && value != '\t') {
                break;
            }
            firstNonWhitespace++;
        }
        return source.substring(lineStart, firstNonWhitespace);
    }

    private boolean sameCode(String left, String right) {
        return left.replaceAll("\\s+", "").equals(right.replaceAll("\\s+", ""));
    }
}
