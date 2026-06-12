package com.dynamicreflector.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class AndroidProjectLocator {
    private final AndroidModuleDetector moduleDetector = new AndroidModuleDetector();
    private final SourceSetDetector sourceSetDetector = new SourceSetDetector();
    private final PackageResolver packageResolver = new PackageResolver();

    public AndroidProject locate(Path projectRoot, String fallbackBasePackage) throws ProjectDetectionException {
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new ProjectDetectionException("Project path does not exist or is not a directory: " + normalizedRoot);
        }

        Path settingsFile = firstExisting(
                normalizedRoot.resolve("settings.gradle"),
                normalizedRoot.resolve("settings.gradle.kts")
        );
        Path rootBuildFile = firstExisting(
                normalizedRoot.resolve("build.gradle"),
                normalizedRoot.resolve("build.gradle.kts")
        );
        Path moduleRoot = moduleDetector.detectAppModule(normalizedRoot);
        Path moduleBuildFile = firstExisting(
                moduleRoot.resolve("build.gradle"),
                moduleRoot.resolve("build.gradle.kts")
        );
        List<Path> sourceRoots = sourceSetDetector.detectJavaSourceRoots(moduleRoot);
        if (sourceRoots.isEmpty()) {
            throw new ProjectDetectionException("No Java source root found under: " + moduleRoot);
        }

        Path manifestPath = moduleRoot.resolve("src").resolve("main").resolve("AndroidManifest.xml");
        if (!Files.exists(manifestPath)) {
            throw new ProjectDetectionException("AndroidManifest.xml not found under module: " + moduleRoot);
        }

        String basePackage = packageResolver.resolveBasePackage(moduleRoot, manifestPath, fallbackBasePackage);
        String moduleName = normalizedRoot.equals(moduleRoot)
                ? moduleRoot.getFileName().toString()
                : normalizedRoot.relativize(moduleRoot).toString();

        return new AndroidProject(
                normalizedRoot,
                settingsFile,
                rootBuildFile,
                moduleRoot,
                moduleBuildFile,
                moduleName,
                sourceRoots,
                manifestPath,
                basePackage
        );
    }

    private Path firstExisting(Path first, Path second) {
        if (Files.exists(first)) {
            return first;
        }
        return Files.exists(second) ? second : null;
    }
}
