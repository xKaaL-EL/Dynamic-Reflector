package com.dynamicreflector.generate;

import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.spoon.MethodInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ApiWrapperGenerator {
    public List<RefactorGeneratedFile> generate(RefactorPlan plan) throws IOException, GeneratedFileConflictException {
        List<RefactorGeneratedFile> generated = new ArrayList<>();
        generated.add(writeIfSameOrMissing(plan.getApiPath(), apiContent(plan)));
        generated.add(writeIfSameOrMissing(plan.getWrapperPath(), wrapperContent(plan)));
        return generated;
    }

    public String apiContent(RefactorPlan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(plan.getApiPackage()).append(";\n\n");
        builder.append("public interface ").append(plan.getApiName()).append(" {\n");
        for (MethodPlan methodPlan : plan.getIncludedMethods()) {
            builder.append("    ").append(interfaceMethodSignature(methodPlan.getMethod())).append(";\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    public String wrapperContent(RefactorPlan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(plan.getWrapperPackage()).append(";\n\n");
        builder.append("import ").append(plan.getApiPackage()).append(".").append(plan.getApiName()).append(";\n\n");
        builder.append("public final class ").append(plan.getWrapperName())
                .append(" implements ").append(plan.getApiName()).append(" {\n");
        for (MethodPlan methodPlan : plan.getIncludedMethods()) {
            MethodInfo method = methodPlan.getMethod();
            builder.append("    @Override\n");
            builder.append("    public ").append(methodSignature(method)).append(" {\n");
            builder.append("        throw new UnsupportedOperationException(")
                    .append("\"Plugin implementation generation is planned for the next batch.\");\n");
            builder.append("    }\n\n");
        }
        if (!plan.getIncludedMethods().isEmpty()) {
            builder.setLength(builder.length() - 1);
        }
        builder.append("}\n");
        return builder.toString();
    }

    private RefactorGeneratedFile writeIfSameOrMissing(Path path, String content)
            throws IOException, GeneratedFileConflictException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            String existing = Files.readString(path, StandardCharsets.UTF_8);
            if (!existing.equals(content)) {
                throw new GeneratedFileConflictException(path);
            }
            return new RefactorGeneratedFile(path, GeneratedFileStatus.UNCHANGED);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return new RefactorGeneratedFile(path, GeneratedFileStatus.CREATED);
    }

    private String interfaceMethodSignature(MethodInfo method) {
        return method.getReturnType() + " " + method.getName()
                + "(" + parameters(method) + ")" + throwsClause(method);
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
