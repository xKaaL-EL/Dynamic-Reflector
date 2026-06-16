package com.dynamicreflector.convert;

import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.refactor.StatelessMethodSafetyAnalyzer;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class OriginalClassConversionPlanner {
    private final StatelessMethodSafetyAnalyzer safetyAnalyzer = new StatelessMethodSafetyAnalyzer();

    public OriginalClassConversionPlan plan(SpoonModelContext model, RefactorPlan refactorPlan)
            throws IOException, ConversionSafetyException {
        Path sourcePath = refactorPlan.getClassInfo().getFilePath();
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new ConversionSafetyException("Original source file was not found for selected class.");
        }

        String originalContent = Files.readString(sourcePath, StandardCharsets.UTF_8);
        String lineSeparator = lineSeparator(originalContent);
        CtType<?> type = findType(model, refactorPlan.getClassInfo().getQualifiedName());
        List<MethodConversionPlan> methodPlans = new ArrayList<>();

        for (MethodPlan methodPlan : refactorPlan.getIncludedMethods()) {
            MethodInfo methodInfo = methodPlan.getMethod();
            CtMethod<?> method = findMethod(type, methodInfo);
            SourcePosition bodyPosition = method.getBody() == null ? null : method.getBody().getPosition();
            if (bodyPosition == null || !bodyPosition.isValidPosition()) {
                throw new ConversionSafetyException("Unable to safely locate method body for " + signature(methodInfo) + ".");
            }

            String replacementBody = replacementBody(originalContent, bodyPosition.getSourceStart(), methodInfo, refactorPlan, lineSeparator);
            String originalBody = originalContent.substring(bodyPosition.getSourceStart(), bodyPosition.getSourceEnd() + 1);
            MethodConversionStatus status;
            if (sameCode(originalBody, replacementBody)) {
                status = MethodConversionStatus.UNCHANGED;
            } else {
                String reason = safetyAnalyzer.unsafeReason(method, methodInfo);
                if (reason != null) {
                    throw new ConversionSafetyException(
                            "Original class delegation conversion is not safe for this class in Batch 4.\n"
                                    + "Reason:\n"
                                    + "method " + signature(methodInfo) + " " + reason + "."
                    );
                }
                status = MethodConversionStatus.CONVERT;
            }

            methodPlans.add(new MethodConversionPlan(
                    methodInfo,
                    bodyPosition.getSourceStart(),
                    bodyPosition.getSourceEnd(),
                    originalBody,
                    replacementBody,
                    status
            ));
        }

        String convertedContent = replaceBodies(originalContent, methodPlans);
        ImportResult apiImport = ensureImport(
                convertedContent,
                refactorPlan.getApiPackage() + "." + refactorPlan.getApiName(),
                shouldAddImports(methodPlans)
        );
        ImportResult managerImport = ensureImport(
                apiImport.content(),
                refactorPlan.getProject().getBasePackage() + ".protection.manager.PremiumFeatureManager",
                shouldAddImports(methodPlans)
        );

        return new OriginalClassConversionPlan(
                refactorPlan,
                sourcePath,
                backupPath(sourcePath),
                originalContent,
                managerImport.content(),
                methodPlans,
                refactorPlan.getSkippedMethods(),
                apiImport.status(),
                managerImport.status(),
                preview(methodPlans)
        );
    }

    private CtType<?> findType(SpoonModelContext model, String qualifiedName) throws ConversionSafetyException {
        return model.getModel()
                .getAllTypes()
                .stream()
                .filter(type -> qualifiedName.equals(type.getQualifiedName()))
                .findFirst()
                .orElseThrow(() -> new ConversionSafetyException("Resolved class was not found in Spoon model."));
    }

    private CtMethod<?> findMethod(CtType<?> type, MethodInfo methodInfo) throws ConversionSafetyException {
        List<CtMethod<?>> matches = type.getMethods()
                .stream()
                .filter(method -> method.getSimpleName().equals(methodInfo.getName()))
                .filter(method -> method.getParameters().size() == methodInfo.getParameterTypes().size())
                .filter(method -> method.getParameters()
                        .stream()
                        .map(parameter -> parameter.getType() == null ? "unknown" : parameter.getType().getQualifiedName())
                        .collect(Collectors.toList())
                        .equals(methodInfo.getParameterTypes()))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new ConversionSafetyException("Unable to safely resolve source method body for " + signature(methodInfo) + ".");
        }
        return matches.get(0);
    }

    private String replaceBodies(String originalContent, List<MethodConversionPlan> methodPlans) {
        StringBuilder builder = new StringBuilder(originalContent);
        methodPlans.stream()
                .filter(plan -> plan.getStatus() == MethodConversionStatus.CONVERT)
                .sorted(Comparator.comparingInt(MethodConversionPlan::getBodyStart).reversed())
                .forEach(plan -> builder.replace(plan.getBodyStart(), plan.getBodyEnd() + 1, plan.getReplacementBody()));
        return builder.toString();
    }

    private ImportResult ensureImport(String content, String qualifiedName, boolean needed) {
        String importLine = "import " + qualifiedName + ";";
        if (content.contains(importLine)) {
            return new ImportResult(content, ImportUpdateStatus.UNCHANGED);
        }
        if (!needed) {
            return new ImportResult(content, ImportUpdateStatus.NOT_NEEDED);
        }

        int lastImportEnd = -1;
        int searchFrom = 0;
        while (true) {
            int importStart = content.indexOf("import ", searchFrom);
            if (importStart < 0) {
                break;
            }
            int importEnd = content.indexOf(';', importStart);
            if (importEnd < 0) {
                break;
            }
            lastImportEnd = importEnd + 1;
            searchFrom = importEnd + 1;
        }
        if (lastImportEnd >= 0) {
            return new ImportResult(
                    content.substring(0, lastImportEnd) + lineSeparator(content) + importLine + content.substring(lastImportEnd),
                    ImportUpdateStatus.ADDED
            );
        }

        int packageEnd = content.indexOf(';');
        if (content.startsWith("package ") && packageEnd >= 0) {
            String lineSeparator = lineSeparator(content);
            return new ImportResult(
                    content.substring(0, packageEnd + 1)
                            + lineSeparator
                            + lineSeparator
                            + importLine
                            + content.substring(packageEnd + 1),
                    ImportUpdateStatus.ADDED
            );
        }
        return new ImportResult(importLine + lineSeparator(content) + lineSeparator(content) + content, ImportUpdateStatus.ADDED);
    }

    private String replacementBody(
            String source,
            int bodyStart,
            MethodInfo method,
            RefactorPlan refactorPlan,
            String lineSeparator
    ) {
        String methodIndent = methodIndent(source, bodyStart);
        String innerIndent = methodIndent + "    ";
        String call = "PremiumFeatureManager.get(" + refactorPlan.getApiName() + ".class)."
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

    private boolean shouldAddImports(List<MethodConversionPlan> methodPlans) {
        return methodPlans.stream().anyMatch(plan -> plan.getStatus() == MethodConversionStatus.CONVERT);
    }

    private boolean sameCode(String left, String right) {
        return normalizeCode(left).equals(normalizeCode(right));
    }

    private String normalizeCode(String code) {
        return code.replaceAll("\\s+", "");
    }

    private String preview(List<MethodConversionPlan> methodPlans) {
        StringBuilder builder = new StringBuilder();
        for (MethodConversionPlan methodPlan : methodPlans) {
            if (methodPlan.getStatus() == MethodConversionStatus.CONVERT) {
                builder.append("- ").append(signature(methodPlan.getMethod())).append(": ")
                        .append(oneLine(methodPlan.getOriginalBody())).append(System.lineSeparator());
                builder.append("+ ").append(signature(methodPlan.getMethod())).append(": ")
                        .append(oneLine(methodPlan.getReplacementBody())).append(System.lineSeparator());
            }
        }
        if (builder.length() == 0) {
            return "No source changes required.";
        }
        return builder.toString().stripTrailing();
    }

    private String oneLine(String value) {
        return value.replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private String lineSeparator(String source) {
        return source.contains("\r\n") ? "\r\n" : "\n";
    }

    private Path backupPath(Path sourcePath) {
        return sourcePath.resolveSibling(sourcePath.getFileName().toString() + ".bak-dynamic-reflector");
    }

    private String signature(MethodInfo method) {
        return method.getName() + "(" + String.join(", ", method.getParameterTypes()) + ")";
    }

    private record ImportResult(String content, ImportUpdateStatus status) {
    }
}
