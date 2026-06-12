package com.dynamicreflector.project;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class AndroidProject {
    private final Path projectRoot;
    private final Path settingsFile;
    private final Path rootBuildFile;
    private final Path moduleRoot;
    private final Path moduleBuildFile;
    private final String moduleName;
    private final List<Path> javaSourceRoots;
    private final Path manifestPath;
    private final String basePackage;

    public AndroidProject(
            Path projectRoot,
            Path settingsFile,
            Path rootBuildFile,
            Path moduleRoot,
            Path moduleBuildFile,
            String moduleName,
            List<Path> javaSourceRoots,
            Path manifestPath,
            String basePackage
    ) {
        this.projectRoot = projectRoot;
        this.settingsFile = settingsFile;
        this.rootBuildFile = rootBuildFile;
        this.moduleRoot = moduleRoot;
        this.moduleBuildFile = moduleBuildFile;
        this.moduleName = moduleName;
        this.javaSourceRoots = Collections.unmodifiableList(javaSourceRoots);
        this.manifestPath = manifestPath;
        this.basePackage = basePackage;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getSettingsFile() {
        return settingsFile;
    }

    public Path getRootBuildFile() {
        return rootBuildFile;
    }

    public Path getModuleRoot() {
        return moduleRoot;
    }

    public Path getModuleBuildFile() {
        return moduleBuildFile;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<Path> getJavaSourceRoots() {
        return javaSourceRoots;
    }

    public Path getManifestPath() {
        return manifestPath;
    }

    public String getBasePackage() {
        return basePackage;
    }
}
