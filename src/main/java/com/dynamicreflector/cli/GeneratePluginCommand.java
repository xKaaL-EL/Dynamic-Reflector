package com.dynamicreflector.cli;

import com.dynamicreflector.generate.GeneratedFileConflictException;
import com.dynamicreflector.generate.GeneratedFileStatus;
import com.dynamicreflector.generate.PluginConfigConflictException;
import com.dynamicreflector.generate.PluginConfigUpdatePlan;
import com.dynamicreflector.generate.PluginConfigUpdater;
import com.dynamicreflector.generate.PluginFeatureLayout;
import com.dynamicreflector.generate.PluginGenerationSafetyException;
import com.dynamicreflector.generate.PluginImplementationGenerator;
import com.dynamicreflector.generate.PluginImplementationPlan;
import com.dynamicreflector.generate.PluginImplementationPlanner;
import com.dynamicreflector.generate.PluginImplementationWritePlan;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;
import com.dynamicreflector.refactor.RefactorPlanner;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.verify.FrameworkVerifier;
import com.dynamicreflector.verify.VerifyResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "generate-plugin", description = "Generate plugin-side implementation source for one safe class.")
public final class GeneratePluginCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--class", required = true, description = "One approved class name.")
    private String className;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Option(names = "--dry-run", description = "Plan plugin implementation generation without writing files.")
    private boolean dryRun;

    @Option(names = "--apply", description = "Generate plugin implementation and update PluginConfig mapping.")
    private boolean apply;

    @Override
    public Integer call() {
        try {
            validateCommandOptions();

            AndroidProject project = locateProject(projectPath, basePackage);
            SpoonModelContext model = loadModel(project);
            JavaClassInfo found = new ClassResolver().resolve(model, className);
            RefactorPlan refactorPlan = new RefactorPlanner().plan(project, found);

            VerifyResult frameworkResult = new FrameworkVerifier().verify(project);
            if (!frameworkResult.isSuccess()) {
                System.err.println("Runtime framework missing.");
                System.err.println("Run:");
                System.err.println("  dynamic-reflector --init " + quoteIfNeeded(project.getProjectRoot()));
                return 1;
            }

            if (!Files.exists(refactorPlan.getApiPath())) {
                System.err.println("API not found.");
                System.err.println("Run:");
                System.err.println("  dynamic-reflector --refactor "
                        + quoteIfNeeded(project.getProjectRoot()) + " "
                        + refactorPlan.getClassInfo().getSimpleName() + " --apply");
                return 1;
            }

            PluginImplementationPlan pluginPlan = new PluginImplementationPlanner().plan(model, refactorPlan);
            PluginImplementationGenerator implementationGenerator = new PluginImplementationGenerator();
            PluginImplementationWritePlan writePlan = implementationGenerator.planWrite(pluginPlan);
            PluginConfigUpdater configUpdater = new PluginConfigUpdater();
            PluginConfigUpdatePlan configUpdate = configUpdater.planUpdate(pluginPlan.getLayout());

            topSeparator();
            section("Plugin Implementation Generation");
            printProject(project);
            System.out.println();
            printPlan(pluginPlan, writePlan, configUpdate);

            if (dryRun) {
                System.out.println();
                System.out.println(ConsoleStyle.success("Dry run complete. No files were written."));
                System.out.println();
                System.out.println(ConsoleStyle.heading("Next:"));
                System.out.println("  dynamic-reflector --generate-plugin "
                        + quoteIfNeeded(project.getProjectRoot()) + " "
                        + refactorPlan.getClassInfo().getSimpleName() + " --apply");
                System.out.println();
                return 0;
            }

            implementationGenerator.write(writePlan);
            configUpdater.write(pluginPlan.getLayout(), configUpdate);

            System.out.println();
            section("Generated Plugin Files");
            System.out.println("  * implementation file " + statusLabel(writePlan.getStatus()) + " "
                    + displayPath(project, writePlan.getPath()));
            System.out.println("  * PluginConfig mapping " + configStatusLabel(configUpdate.getMappingStatus()));
            System.out.println("  * allowed prefix " + configStatusLabel(configUpdate.getAllowedPrefixStatus()));
            System.out.println();
            System.out.println("Original class was not modified.");
            System.out.println("Caller classes were not modified.");
            System.out.println("Plugin APK/module is not wired yet.");
            System.out.println("Wrapper conversion is planned for Batch 4.");
            System.out.println();
            System.out.println(ConsoleStyle.heading("Next:"));
            System.out.println("  dynamic-reflector --verify "
                    + quoteIfNeeded(project.getProjectRoot()) + " "
                    + refactorPlan.getClassInfo().getSimpleName());
            System.out.println();
            return 0;
        } catch (PluginGenerationSafetyException | PluginConfigConflictException | GeneratedFileConflictException e) {
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

    private void printPlan(
            PluginImplementationPlan pluginPlan,
            PluginImplementationWritePlan writePlan,
            PluginConfigUpdatePlan configUpdate
    ) {
        RefactorPlan refactorPlan = pluginPlan.getLayout().getRefactorPlan();
        PluginFeatureLayout layout = pluginPlan.getLayout();

        System.out.println("Selected class: " + className);
        System.out.println("Resolved: " + refactorPlan.getClassInfo().getQualifiedName());
        System.out.println("API status: found");
        System.out.println("Plugin implementation path: " + displayPath(refactorPlan.getProject(), layout.getImplementationPath()));
        System.out.println("PluginConfig mapping planned: " + refactorPlan.getApiName()
                + ".class -> \"" + layout.getImplementationQualifiedName() + "\"");
        System.out.println("Allowed implementation prefix: " + layout.getAllowedImplementationPrefix()
                + " (" + configStatusLabel(configUpdate.getAllowedPrefixStatus()) + ")");
        System.out.println("Implementation file status: " + statusLabel(writePlan.getStatus()));
        System.out.println();
        printMethods(pluginPlan);
    }

    private void printMethods(PluginImplementationPlan pluginPlan) {
        System.out.println(ConsoleStyle.heading("Methods planned for copy:"));
        if (pluginPlan.getMethodsToCopy().isEmpty()) {
            System.out.println("  None");
        } else {
            pluginPlan.getMethodsToCopy().forEach(methodCopy ->
                    System.out.println("  * " + MethodFormatter.signature(methodCopy.getMethod(), verbose))
            );
        }
        System.out.println();
        System.out.println(ConsoleStyle.heading("Skipped methods:"));
        if (pluginPlan.getSkippedMethods().isEmpty()) {
            System.out.println("  None");
        } else {
            for (MethodPlan methodPlan : pluginPlan.getSkippedMethods()) {
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose)
                        + " [" + methodPlan.getReason() + "]");
            }
        }
    }

    private String statusLabel(GeneratedFileStatus status) {
        return status == GeneratedFileStatus.CREATED ? "created" : "unchanged";
    }

    private String configStatusLabel(GeneratedFileStatus status) {
        return status == GeneratedFileStatus.CREATED ? "added" : "unchanged";
    }
}
