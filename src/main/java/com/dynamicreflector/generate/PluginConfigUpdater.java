package com.dynamicreflector.generate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginConfigUpdater {
    public PluginConfigUpdatePlan planUpdate(PluginFeatureLayout layout)
            throws IOException, PluginConfigConflictException {
        Path path = layout.getPluginConfigPath();
        String content = Files.readString(path, StandardCharsets.UTF_8);

        PrefixUpdate prefixUpdate = ensureAllowedPrefix(content, layout.getAllowedImplementationPrefix());
        MappingUpdate mappingUpdate = ensureMapping(prefixUpdate.content(), layout);
        String withImport = ensureApiImport(mappingUpdate.content(), layout.getApiQualifiedName());

        return new PluginConfigUpdatePlan(
                withImport,
                mappingUpdate.status(),
                prefixUpdate.status()
        );
    }

    public void write(PluginFeatureLayout layout, PluginConfigUpdatePlan updatePlan) throws IOException {
        Files.writeString(layout.getPluginConfigPath(), updatePlan.getContent(), StandardCharsets.UTF_8);
    }

    public boolean containsMapping(Path pluginConfigPath, PluginFeatureLayout layout) throws IOException {
        if (!Files.exists(pluginConfigPath)) {
            return false;
        }
        String content = Files.readString(pluginConfigPath, StandardCharsets.UTF_8);
        return findRegistration(content, layout) != null
                && layout.getImplementationQualifiedName().equals(findRegistration(content, layout).implementationClass());
    }

    public boolean containsAllowedPrefix(Path pluginConfigPath, String allowedPrefix) throws IOException {
        if (!Files.exists(pluginConfigPath)) {
            return false;
        }
        return Files.readString(pluginConfigPath, StandardCharsets.UTF_8)
                .contains("\"" + allowedPrefix + "\"");
    }

    private PrefixUpdate ensureAllowedPrefix(String content, String allowedPrefix) throws PluginConfigConflictException {
        Pattern pattern = Pattern.compile("return\\s+Arrays\\.asList\\(([^;]*)\\);", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new PluginConfigConflictException(
                    "Unable to update allowed implementation prefix in PluginConfig."
            );
        }
        String existingArgs = matcher.group(1);
        if (existingArgs.contains("\"" + allowedPrefix + "\"")) {
            return new PrefixUpdate(content, GeneratedFileStatus.UNCHANGED);
        }
        String trimmed = existingArgs.trim();
        String nextArgs = trimmed.isEmpty()
                ? "\"" + allowedPrefix + "\""
                : existingArgs + ", \"" + allowedPrefix + "\"";
        String updated = content.substring(0, matcher.start(1))
                + nextArgs
                + content.substring(matcher.end(1));
        return new PrefixUpdate(updated, GeneratedFileStatus.CREATED);
    }

    private MappingUpdate ensureMapping(String content, PluginFeatureLayout layout) throws PluginConfigConflictException {
        Registration existing = findRegistration(content, layout);
        if (existing != null) {
            if (!layout.getImplementationQualifiedName().equals(existing.implementationClass())) {
                throw new PluginConfigConflictException(
                        "Conflicting PluginConfig mapping for " + layout.getRefactorPlan().getApiName()
                                + ": " + existing.implementationClass()
                );
            }
            return new MappingUpdate(content, GeneratedFileStatus.UNCHANGED);
        }

        Pattern methodPattern = Pattern.compile(
                "(public\\s+static\\s+void\\s+registerMappings\\s*\\(\\s*PluginRegistry\\s+registry\\s*\\)\\s*\\{)([\\s\\S]*?)(\\n\\s*\\})"
        );
        Matcher matcher = methodPattern.matcher(content);
        if (!matcher.find()) {
            throw new PluginConfigConflictException("Unable to update PluginConfig.registerMappings.");
        }

        String body = matcher.group(2);
        String mappingLine = "\n        registry.register("
                + layout.getRefactorPlan().getApiName()
                + ".class, \""
                + layout.getImplementationQualifiedName()
                + "\", \""
                + layout.getFeatureName()
                + "\");";
        String updated = content.substring(0, matcher.start(2))
                + mappingLine
                + body
                + content.substring(matcher.end(2));
        return new MappingUpdate(updated, GeneratedFileStatus.CREATED);
    }

    private Registration findRegistration(String content, PluginFeatureLayout layout) {
        String apiSimple = Pattern.quote(layout.getRefactorPlan().getApiName());
        String apiQualified = Pattern.quote(layout.getApiQualifiedName());
        Pattern pattern = Pattern.compile(
                "registry\\.register\\(\\s*(?:" + apiSimple + "|" + apiQualified + ")\\.class\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\)",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return new Registration(matcher.group(1), matcher.group(2));
    }

    private String ensureApiImport(String content, String apiQualifiedName) {
        String importLine = "import " + apiQualifiedName + ";";
        if (content.contains(importLine)) {
            return content;
        }

        Pattern importPattern = Pattern.compile("(?m)^import .+;$");
        Matcher matcher = importPattern.matcher(content);
        int insertAt = -1;
        while (matcher.find()) {
            insertAt = matcher.end();
        }
        if (insertAt >= 0) {
            return content.substring(0, insertAt) + "\n" + importLine + content.substring(insertAt);
        }

        Pattern packagePattern = Pattern.compile("(?m)^package .+;$");
        Matcher packageMatcher = packagePattern.matcher(content);
        if (packageMatcher.find()) {
            int packageEnd = packageMatcher.end();
            return content.substring(0, packageEnd) + "\n\n" + importLine + content.substring(packageEnd);
        }
        return importLine + "\n\n" + content;
    }

    private record PrefixUpdate(String content, GeneratedFileStatus status) {
    }

    private record MappingUpdate(String content, GeneratedFileStatus status) {
    }

    private record Registration(String implementationClass, String featureName) {
    }
}
