package com.dynamicreflector.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackageResolver {
    public String resolveBasePackage(Path moduleRoot, Path manifestPath, String fallbackBasePackage)
            throws ProjectDetectionException {
        String namespace = readGradleValue(moduleRoot, "namespace");
        if (isPackageLike(namespace)) {
            return namespace;
        }

        String applicationId = readGradleValue(moduleRoot, "applicationId");
        if (isPackageLike(applicationId)) {
            return applicationId;
        }

        String manifestPackage = readManifestPackage(manifestPath);
        if (isPackageLike(manifestPackage)) {
            return manifestPackage;
        }

        if (isPackageLike(fallbackBasePackage)) {
            return fallbackBasePackage;
        }

        throw new ProjectDetectionException(
                "Unable to detect Android namespace/applicationId/package. Re-run with --base-package <package.name>."
        );
    }

    private String readGradleValue(Path moduleRoot, String key) {
        String fromGroovy = readGradleFileValue(moduleRoot.resolve("build.gradle"), key);
        if (fromGroovy != null) {
            return fromGroovy;
        }
        return readGradleFileValue(moduleRoot.resolve("build.gradle.kts"), key);
    }

    private String readGradleFileValue(Path gradleFile, String key) {
        if (!Files.exists(gradleFile)) {
            return null;
        }
        try {
            String text = new String(Files.readAllBytes(gradleFile), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(key) + "\\s*(?:=\\s*)?[\"']([^\"']+)[\"']");
            Matcher matcher = pattern.matcher(text);
            return matcher.find() ? matcher.group(1).trim() : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private String readManifestPackage(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(manifestPath.toFile());
            Element manifest = document.getDocumentElement();
            return manifest.hasAttribute("package") ? manifest.getAttribute("package").trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPackageLike(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
}
