package com.dynamicreflector.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "refactor", description = "Refactor one approved class. Disabled until the next approved batch.")
public final class RefactorCommand implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--class", required = true, description = "One approved class name.")
    private String className;

    @Option(names = "--strategy", defaultValue = "wrapper", description = "Refactor strategy. MVP default is wrapper.")
    private String strategy;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Option(names = "--update-proguard", description = "Later: add a small marked R8/ProGuard block.")
    private boolean updateProguard;

    @Override
    public Integer call() {
        System.out.println("Refactor command parsed successfully but is intentionally not implemented in this batch.");
        System.out.println("Requested project: " + projectPath);
        System.out.println("Requested class: " + className);
        System.out.println("Requested strategy: " + strategy);
        System.out.println("No files were changed. One-class wrapper refactor requires the next approval.");
        return 0;
    }
}
