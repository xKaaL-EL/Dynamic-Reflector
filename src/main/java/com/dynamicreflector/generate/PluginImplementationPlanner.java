package com.dynamicreflector.generate;

import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.refactor.StatelessMethodSafetyAnalyzer;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class PluginImplementationPlanner {
    private final StatelessMethodSafetyAnalyzer safetyAnalyzer = new StatelessMethodSafetyAnalyzer();

    public PluginImplementationPlan plan(SpoonModelContext model, RefactorPlan refactorPlan)
            throws IOException, PluginGenerationSafetyException {
        PluginFeatureLayout layout = PluginFeatureLayout.from(refactorPlan);
        requireGeneratedApi(refactorPlan);

        CtType<?> type = findType(model, refactorPlan.getClassInfo().getQualifiedName());
        List<PluginMethodCopy> methodsToCopy = new ArrayList<>();
        for (MethodPlan methodPlan : refactorPlan.getIncludedMethods()) {
            MethodInfo methodInfo = methodPlan.getMethod();
            requireMethodInApi(refactorPlan, methodInfo);
            CtMethod<?> method = findMethod(type, methodInfo);
            String reason = safetyAnalyzer.unsafeReason(method, methodInfo);
            if (reason != null) {
                throw new PluginGenerationSafetyException(
                        "Plugin implementation generation is not safe for this class in Batch 3.\n"
                                + "Reason:\n"
                                + "method " + signature(methodInfo) + " " + reason + ".\n"
                                + "Supported in a later wrapper-conversion/state-transfer batch."
                );
            }
            methodsToCopy.add(new PluginMethodCopy(methodInfo, method.getBody().toString()));
        }

        return new PluginImplementationPlan(layout, methodsToCopy, refactorPlan.getSkippedMethods());
    }

    private void requireGeneratedApi(RefactorPlan refactorPlan) throws IOException, PluginGenerationSafetyException {
        if (!Files.exists(refactorPlan.getApiPath())) {
            throw new PluginGenerationSafetyException("API not found.");
        }
    }

    private void requireMethodInApi(RefactorPlan refactorPlan, MethodInfo method)
            throws IOException, PluginGenerationSafetyException {
        String apiContent = Files.readString(refactorPlan.getApiPath(), StandardCharsets.UTF_8);
        String signature = method.getReturnType() + " " + method.getName()
                + "(" + parameters(method) + ")" + throwsClause(method) + ";";
        if (!apiContent.contains(signature)) {
            throw new PluginGenerationSafetyException(
                    "Generated API does not contain method " + signature(method) + "."
            );
        }
    }

    private CtType<?> findType(SpoonModelContext model, String qualifiedName) throws PluginGenerationSafetyException {
        return model.getModel()
                .getAllTypes()
                .stream()
                .filter(type -> qualifiedName.equals(type.getQualifiedName()))
                .findFirst()
                .orElseThrow(() -> new PluginGenerationSafetyException("Resolved class was not found in Spoon model."));
    }

    private CtMethod<?> findMethod(CtType<?> type, MethodInfo methodInfo) throws PluginGenerationSafetyException {
        List<CtMethod<?>> matches = type.getMethods()
                .stream()
                .filter(method -> method.getSimpleName().equals(methodInfo.getName()))
                .filter(method -> method.getParameters().size() == methodInfo.getParameterTypes().size())
                .filter(method -> parameterTypes(method).equals(methodInfo.getParameterTypes()))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new PluginGenerationSafetyException(
                    "Unable to safely resolve source method body for " + signature(methodInfo) + "."
            );
        }
        return matches.get(0);
    }

    private List<String> parameterTypes(CtMethod<?> method) {
        return method.getParameters()
                .stream()
                .map(parameter -> parameter.getType() == null ? "unknown" : parameter.getType().getQualifiedName())
                .collect(Collectors.toList());
    }

    private String parameters(MethodInfo method) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            String name = i < method.getParameterNames().size() ? method.getParameterNames().get(i) : "arg" + i;
            parts.add(method.getParameterTypes().get(i) + " " + name);
        }
        return String.join(", ", parts);
    }

    private String throwsClause(MethodInfo method) {
        if (method.getThrownTypes().isEmpty()) {
            return "";
        }
        return " throws " + String.join(", ", method.getThrownTypes());
    }

    private String signature(MethodInfo method) {
        return method.getName() + "(" + String.join(", ", method.getParameterTypes()) + ")";
    }

}
