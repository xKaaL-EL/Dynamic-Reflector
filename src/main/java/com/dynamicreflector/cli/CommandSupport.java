package com.dynamicreflector.cli;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.project.AndroidProjectLocator;
import com.dynamicreflector.project.ProjectDetectionException;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.spoon.SpoonModelLoader;

import java.io.IOException;
import java.nio.file.Path;

abstract class CommandSupport {
    protected AndroidProject locateProject(Path projectPath, String basePackage) throws ProjectDetectionException {
        return new AndroidProjectLocator().locate(projectPath, basePackage);
    }

    protected SpoonModelContext loadModel(AndroidProject project) throws IOException {
        return new SpoonModelLoader().load(project);
    }

    protected void printProject(AndroidProject project) {
        System.out.println("Project: " + project.getProjectRoot());
        System.out.println("Settings file: " + printable(project.getSettingsFile()));
        System.out.println("Root build file: " + printable(project.getRootBuildFile()));
        System.out.println("Module: " + project.getModuleName() + " (" + project.getModuleRoot() + ")");
        System.out.println("Module build file: " + printable(project.getModuleBuildFile()));
        System.out.println("Base package: " + project.getBasePackage());
        System.out.println("Manifest: " + project.getManifestPath());
        System.out.println("Java source roots:");
        for (Path root : project.getJavaSourceRoots()) {
            System.out.println("  - " + root);
        }
    }

    protected int fail(Exception e) {
        System.err.println("Error: " + e.getMessage());
        return 1;
    }

    private String printable(Path path) {
        return path == null ? "not found" : path.toString();
    }
}
