package com.dynamicreflector.generate;

import com.dynamicreflector.spoon.MethodInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PluginImplementationGenerator {
    public PluginImplementationWritePlan planWrite(PluginImplementationPlan plan)
            throws IOException, GeneratedFileConflictException {
        String content = implementationContent(plan);
        if (Files.exists(plan.getLayout().getImplementationPath())) {
            String existing = Files.readString(plan.getLayout().getImplementationPath(), StandardCharsets.UTF_8);
            if (!existing.equals(content)) {
                throw new GeneratedFileConflictException(plan.getLayout().getImplementationPath());
            }
            return new PluginImplementationWritePlan(
                    plan.getLayout().getImplementationPath(),
                    content,
                    GeneratedFileStatus.UNCHANGED
            );
        }
        return new PluginImplementationWritePlan(
                plan.getLayout().getImplementationPath(),
                content,
                GeneratedFileStatus.CREATED
        );
    }

    public void write(PluginImplementationWritePlan writePlan) throws IOException {
        Files.createDirectories(writePlan.getPath().getParent());
        if (writePlan.getStatus() == GeneratedFileStatus.CREATED) {
            Files.writeString(writePlan.getPath(), writePlan.getContent(), StandardCharsets.UTF_8);
        }
    }

    private String implementationContent(PluginImplementationPlan plan) {
        StringBuilder builder = new StringBuilder();
        PluginFeatureLayout layout = plan.getLayout();
        builder.append("package ").append(layout.getImplementationPackage()).append(";\n\n");
        builder.append("import ").append(layout.getApiQualifiedName()).append(";\n\n");
        builder.append("public final class ").append(layout.getImplementationName())
                .append(" implements ").append(layout.getRefactorPlan().getApiName()).append(" {\n");
        for (PluginMethodCopy methodCopy : plan.getMethodsToCopy()) {
            builder.append("    @Override\n");
            builder.append("    public ").append(methodSignature(methodCopy.getMethod())).append(" ");
            builder.append(formatBody(methodCopy.getBody())).append("\n\n");
        }
        if (!plan.getMethodsToCopy().isEmpty()) {
            builder.setLength(builder.length() - 1);
        }
        builder.append("}\n");
        return builder.toString();
    }

    private String methodSignature(MethodInfo method) {
        return method.getReturnType() + " " + method.getName()
                + "(" + parameters(method) + ")" + throwsClause(method);
    }

    private String parameters(MethodInfo method) {
        List<String> parts = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            String name = safeParameterName(
                    i < method.getParameterNames().size() ? method.getParameterNames().get(i) : null,
                    i,
                    usedNames
            );
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

    private String formatBody(String body) {
        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n');
                builder.append("    ");
            }
            builder.append(lines[i]);
        }
        return builder.toString();
    }

    private String safeParameterName(String candidate, int index, Set<String> usedNames) {
        String name = candidate == null ? "" : candidate.trim();
        if (!name.matches("[A-Za-z_][A-Za-z0-9_]*") || isJavaKeyword(name)) {
            name = "arg" + index;
        }
        while (usedNames.contains(name)) {
            name = name + index;
        }
        usedNames.add(name);
        return name;
    }

    private boolean isJavaKeyword(String value) {
        return Set.of(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
                "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
                "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
                "throw", "throws", "transient", "try", "void", "volatile", "while"
        ).contains(value);
    }
}
