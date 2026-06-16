package com.dynamicreflector.cli;

import com.dynamicreflector.convert.BackupStatus;
import com.dynamicreflector.convert.ConversionPrecondition;
import com.dynamicreflector.convert.ConversionPreconditionChecker;
import com.dynamicreflector.convert.ConversionPreconditionResult;
import com.dynamicreflector.convert.ConversionSafetyException;
import com.dynamicreflector.convert.ImportUpdateStatus;
import com.dynamicreflector.convert.MethodConversionPlan;
import com.dynamicreflector.convert.MethodConversionStatus;
import com.dynamicreflector.convert.OriginalClassConversionPlan;
import com.dynamicreflector.convert.OriginalClassConversionPlanner;
import com.dynamicreflector.convert.OriginalClassConversionResult;
import com.dynamicreflector.convert.OriginalClassConverter;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.refactor.RefactorPlanner;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "convert-wrapper", description = "Convert one original safe class to delegate through PremiumFeatureManager.")
public final class ConvertWrapperCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--class", required = true, description = "One approved class name.")
    private String className;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Option(names = "--dry-run", description = "Plan original class conversion without writing files.")
    private boolean dryRun;

    @Option(names = "--apply", description = "Replace safe original method bodies with plugin delegation.")
    private boolean apply;

    @Override
    public Integer call() {
        try {
            validateCommandOptions();

            AndroidProject project = locateProject(projectPath, basePackage);
            SpoonModelContext model = loadModel(project);
            JavaClassInfo found = new ClassResolver().resolve(model, className);
            RefactorPlan refactorPlan = new RefactorPlanner().plan(project, found);

            ConversionPreconditionResult preconditions = new ConversionPreconditionChecker().check(refactorPlan);
            if (!preconditions.isSuccess()) {
                ConversionPrecondition failure = preconditions.firstFailure();
                System.err.println(failure.getLabel() + " missing.");
                System.err.println("Run:");
                System.err.println("  " + failure.getCommand());
                return 1;
            }

            OriginalClassConversionPlan plan = new OriginalClassConversionPlanner().plan(model, refactorPlan);

            topSeparator();
            section("Original Class Delegation Conversion");
            printProject(project);
            System.out.println();
            printPlan(plan, preconditions);

            if (dryRun) {
                System.out.println();
                System.out.println(ConsoleStyle.success("Dry run complete. No files were written."));
                System.out.println();
                System.out.println(ConsoleStyle.heading("Next:"));
                System.out.println("  dynamic-reflector --convert-wrapper "
                        + quoteIfNeeded(project.getProjectRoot()) + " "
                        + refactorPlan.getClassInfo().getSimpleName() + " --apply");
                System.out.println();
                return 0;
            }

            OriginalClassConversionResult result = new OriginalClassConverter().apply(plan);
            printApplyResult(plan, result);
            return 0;
        } catch (ConversionSafetyException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private void validateCommandOptions() {
        if (dryRun == apply) {
            throw new IllegalArgumentException("Pass exactly one of --dry-run or --apply.");
        }
    }

    private void printPlan(OriginalClassConversionPlan plan, ConversionPreconditionResult preconditions) {
        RefactorPlan refactorPlan = plan.getRefactorPlan();
        System.out.println("Selected class: " + className);
        System.out.println("Resolved: " + refactorPlan.getClassInfo().getQualifiedName());
        System.out.println();
        System.out.println(ConsoleStyle.heading("Preconditions:"));
        for (ConversionPrecondition precondition : preconditions.getChecks()) {
            System.out.println("  * " + precondition.getLabel() + ": "
                    + (precondition.isSuccess() ? ConsoleStyle.success("OK") : ConsoleStyle.error("missing")));
        }
        System.out.println();
        printMethods(plan);
        System.out.println();
        System.out.println(ConsoleStyle.heading("Preview:"));
        for (String line : plan.getPreview().split("\\R")) {
            System.out.println("  " + line);
        }
    }

    private void printMethods(OriginalClassConversionPlan plan) {
        System.out.println(ConsoleStyle.heading("Methods to convert:"));
        boolean anyConvert = false;
        for (MethodConversionPlan methodPlan : plan.getMethodPlans()) {
            if (methodPlan.getStatus() == MethodConversionStatus.CONVERT) {
                anyConvert = true;
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose));
            }
        }
        if (!anyConvert) {
            System.out.println("  None");
        }

        System.out.println();
        System.out.println(ConsoleStyle.heading("Methods unchanged:"));
        boolean anyUnchanged = false;
        for (MethodConversionPlan methodPlan : plan.getMethodPlans()) {
            if (methodPlan.getStatus() == MethodConversionStatus.UNCHANGED) {
                anyUnchanged = true;
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose));
            }
        }
        if (!anyUnchanged) {
            System.out.println("  None");
        }

        System.out.println();
        System.out.println(ConsoleStyle.heading("Skipped methods:"));
        if (plan.getSkippedMethods().isEmpty()) {
            System.out.println("  None");
        } else {
            for (MethodPlan methodPlan : plan.getSkippedMethods()) {
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose)
                        + " [" + methodPlan.getReason() + "]");
            }
        }
    }

    private void printApplyResult(OriginalClassConversionPlan plan, OriginalClassConversionResult result) {
        System.out.println();
        section("Conversion Result");
        System.out.println("  * backup " + backupStatus(result.getBackupStatus()) + " "
                + displayPath(plan.getRefactorPlan().getProject(), plan.getBackupPath()));
        System.out.println("  * original class "
                + (result.isSourceChanged() ? ConsoleStyle.success("converted") : ConsoleStyle.success("already converted"))
                + " " + displayPath(plan.getRefactorPlan().getProject(), plan.getSourcePath()));
        System.out.println("  * methods converted: " + plan.convertedMethodCount());
        System.out.println("  * methods unchanged: " + plan.unchangedMethodCount());
        System.out.println("  * API import " + importStatus(plan.getApiImportStatus()));
        System.out.println("  * manager import " + importStatus(plan.getManagerImportStatus()));
        System.out.println();
        System.out.println("Caller classes were not modified.");
        System.out.println("Plugin APK/module is not wired yet.");
        System.out.println("Build validation is still required.");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Next:"));
        System.out.println("  dynamic-reflector --verify "
                + quoteIfNeeded(plan.getRefactorPlan().getProject().getProjectRoot()) + " "
                + plan.getRefactorPlan().getClassInfo().getSimpleName());
        System.out.println();
    }

    private String backupStatus(BackupStatus status) {
        return switch (status) {
            case CREATED -> "created";
            case UNCHANGED, NOT_NEEDED -> "unchanged";
        };
    }

    private String importStatus(ImportUpdateStatus status) {
        return switch (status) {
            case ADDED -> "added";
            case UNCHANGED, NOT_NEEDED -> "unchanged";
        };
    }
}
