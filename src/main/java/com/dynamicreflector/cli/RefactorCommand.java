package com.dynamicreflector.cli;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.generate.ApiWrapperGenerator;
import com.dynamicreflector.generate.RefactorGeneratedFile;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.refactor.RefactorPlanner;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.verify.FrameworkVerifier;
import com.dynamicreflector.verify.VerifyResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "refactor", description = "Prepare API and wrapper files for one safe class.")
public final class RefactorCommand extends CommandSupport implements Callable<Integer> {
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

    @Option(names = "--dry-run", description = "Plan the API/wrapper preparation without writing files.")
    private boolean dryRun;

    @Option(names = "--apply", description = "Generate API and compile-safe wrapper placeholder files.")
    private boolean apply;

    @Override
    public Integer call() {
        try {
            validateCommandOptions();

            AndroidProject project = locateProject(projectPath, basePackage);
            SpoonModelContext model = loadModel(project);
            JavaClassInfo found = new ClassResolver().resolve(model, className);
            RefactorPlan plan = new RefactorPlanner().plan(project, found);
            printPlan(plan);

            if (dryRun) {
                System.out.println();
                System.out.println(ConsoleStyle.success("Dry run complete. No files were written."));
                System.out.println();
                System.out.println(ConsoleStyle.heading("Next:"));
                System.out.println("  dynamic-reflector --refactor "
                        + quoteIfNeeded(project.getProjectRoot()) + " " + plan.getClassInfo().getSimpleName() + " --apply");
                return 0;
            }

            VerifyResult frameworkResult = new FrameworkVerifier().verify(project);
            if (!frameworkResult.isSuccess()) {
                System.err.println();
                System.err.println("Minimal framework files are missing. Run apply-framework --minimal first.");
                frameworkResult.getMessages().forEach(message -> System.err.println("  - " + message));
                return 1;
            }

            System.out.println();
            section("Generated Files");
            for (RefactorGeneratedFile generatedFile : new ApiWrapperGenerator().generate(plan)) {
                String status = generatedFile.getStatus().name().toLowerCase();
                System.out.println("  * " + ConsoleStyle.success(status) + " "
                        + displayPath(project, generatedFile.getPath()));
            }
            System.out.println();
            System.out.println("Original class was not modified.");
            System.out.println("Caller classes were not modified.");
            System.out.println("Plugin implementation generation is planned for the next batch.");
            System.out.println();
            System.out.println(ConsoleStyle.heading("Next:"));
            System.out.println("  dynamic-reflector --verify " + quoteIfNeeded(project.getProjectRoot()));
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private void validateCommandOptions() {
        if (!"wrapper".equals(strategy)) {
            throw new IllegalArgumentException("Only --strategy wrapper is supported in Batch 2.");
        }
        if (updateProguard) {
            throw new IllegalArgumentException("--update-proguard is not implemented in Batch 2.");
        }
        if (dryRun == apply) {
            throw new IllegalArgumentException("Pass exactly one of --dry-run or --apply.");
        }
    }

    private void printPlan(RefactorPlan plan) {
        JavaClassInfo classInfo = plan.getClassInfo();
        ClassClassification classification = plan.getClassification();

        topSeparator();
        section("Refactor Preparation");
        printProject(plan.getProject());
        System.out.println();
        System.out.println("Selected class: " + classInfo.getQualifiedName());
        if (verbose) {
            System.out.println("Source file: " + classInfo.getFilePath());
        }
        System.out.println("Classification: " + classification.getBucket());
        System.out.println("Reasons: " + String.join("; ", classification.getReasons()));
        System.out.println("Strategy: wrapper");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Planned generated files:"));
        System.out.println("  * API: " + displayPath(plan.getProject(), plan.getApiPath()));
        System.out.println("  * Wrapper: " + displayPath(plan.getProject(), plan.getWrapperPath()));
        System.out.println();
        printIncludedMethods(plan);
        System.out.println();
        printSkippedMethods(plan);
    }

    private String methodLine(MethodInfo method) {
        return MethodFormatter.signature(method, verbose);
    }

    private void printIncludedMethods(RefactorPlan plan) {
        System.out.println(ConsoleStyle.heading("Included methods:"));
        if (plan.getIncludedMethods().isEmpty()) {
            System.out.println("  None");
            return;
        }
        for (MethodPlan methodPlan : plan.getIncludedMethods()) {
            System.out.println("  * " + methodLine(methodPlan.getMethod()));
        }
    }

    private void printSkippedMethods(RefactorPlan plan) {
        System.out.println(ConsoleStyle.heading("Skipped methods:"));
        if (plan.getSkippedMethods().isEmpty()) {
            System.out.println("  None");
            return;
        }
        for (MethodPlan methodPlan : plan.getSkippedMethods()) {
            System.out.println("  * " + methodLine(methodPlan.getMethod()) + " [" + methodPlan.getReason() + "]");
        }
    }
}
