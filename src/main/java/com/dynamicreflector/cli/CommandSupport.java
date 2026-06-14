package com.dynamicreflector.cli;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.project.AndroidProjectLocator;
import com.dynamicreflector.project.ProjectDetectionException;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.spoon.SpoonModelLoader;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;

abstract class CommandSupport {
    @Option(names = "--verbose", description = "Show full paths and detailed project metadata.")
    protected boolean verbose;

    protected AndroidProject locateProject(Path projectPath, String basePackage) throws ProjectDetectionException {
        return new AndroidProjectLocator().locate(projectPath, basePackage);
    }

    protected SpoonModelContext loadModel(AndroidProject project) throws IOException {
        return new SpoonModelLoader().load(project);
    }

    protected void printProject(AndroidProject project) {
        if (!verbose) {
            printProjectSummary(project);
            return;
        }
        section("Project Details");
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

    protected void printProjectSummary(AndroidProject project) {
        System.out.println("Project: " + project.getProjectRoot().getFileName());
        System.out.println("Module: " + project.getModuleName());
        System.out.println("Base package: " + project.getBasePackage());
    }

    protected String displayPath(AndroidProject project, Path path) {
        if (verbose || path == null) {
            return printable(path);
        }
        Path root = project.getProjectRoot();
        try {
            return root.relativize(path.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return path.toString();
        }
    }

    protected int fail(Exception e) {
        System.err.println(ConsoleStyle.error("Error: ") + e.getMessage());
        return 1;
    }

    protected void topSeparator() {
        System.out.println();
        System.out.println(ConsoleStyle.green("===================================================="));
        System.out.println(ConsoleStyle.green("Dynamic Reflector"));
        System.out.println(ConsoleStyle.green("================="));
        System.out.println();
    }

    protected void section(String title) {
        System.out.println(ConsoleStyle.heading(title));
        System.out.println("----------------------------------------");
    }

    protected String quoteIfNeeded(Path path) {
        String value = path.toString();
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }

    protected String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private String printable(Path path) {
        return path == null ? "not found" : path.toString();
    }
}
