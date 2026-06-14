package com.dynamicreflector.cli;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlanner;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonModelContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "analyze-class", description = "Print method and wrapper suitability details for one class.")
public final class AnalyzeClassCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--class", required = true, description = "Fully qualified, simple, or suffix class name.")
    private String className;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Override
    public Integer call() {
        try {
            AndroidProject project = locateProject(projectPath, basePackage);
            SpoonModelContext model = loadModel(project);
            JavaClassInfo info = new ClassResolver().resolve(model, className);
            ClassClassification classification = new FileClassifier().classify(info);
            RefactorPlanner planner = new RefactorPlanner();
            List<MethodPlan> methodPlans = planner.planMethods(info);
            List<MethodPlan> supported = methodPlans.stream().filter(MethodPlan::isIncluded).collect(Collectors.toList());
            List<MethodPlan> skipped = methodPlans.stream().filter(method -> !method.isIncluded()).collect(Collectors.toList());
            boolean wrapperPossible = classification.isWrapperPossible()
                    && !supported.isEmpty()
                    && !planner.hasUnsupportedPublicInstanceMethod(methodPlans);

            topSeparator();
            section("Class Inspection");
            System.out.println("Class: " + info.getSimpleName());
            System.out.println("Resolved: " + info.getQualifiedName());
            if (verbose) {
                System.out.println("File: " + info.getFilePath());
                System.out.println("Superclass: " + info.getSuperClassName());
            }
            System.out.println("Classification: " + classification.getBucket());
            if (verbose) {
                System.out.println("Reasons: " + String.join("; ", classification.getReasons()));
            }
            System.out.println("Wrapper possible: " + yesNo(wrapperPossible));
            System.out.println();
            printSupportedMethods(supported);
            printSkippedMethods(skipped);
            printStaticMethods(info.getMethods());
            printNext(project, info, wrapperPossible);
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private String methodLine(MethodInfo method) {
        if (!verbose) {
            return MethodFormatter.signature(method, false);
        }
        String visibility = method.isPublicMethod()
                ? "public"
                : method.isProtectedMethod()
                ? "protected"
                : method.isPrivateMethod()
                ? "private"
                : "package";
        String staticText = method.isStaticMethod() ? " static" : "";
        return visibility + staticText + " " + method.getReturnType() + " "
                + method.getName() + "(" + String.join(", ", method.getParameterTypes()) + ")"
                + (method.hasBody() ? "" : " [no body]");
    }

    private void printSupportedMethods(List<MethodPlan> supported) {
        System.out.println(ConsoleStyle.heading("Supported methods:"));
        if (supported.isEmpty()) {
            System.out.println("  None");
        } else {
            for (MethodPlan methodPlan : supported) {
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose));
            }
        }
        System.out.println();
    }

    private void printSkippedMethods(List<MethodPlan> skipped) {
        System.out.println(ConsoleStyle.heading("Skipped methods:"));
        if (skipped.isEmpty()) {
            System.out.println("  None");
        } else {
            for (MethodPlan methodPlan : skipped) {
                System.out.println("  * " + MethodFormatter.signature(methodPlan.getMethod(), verbose)
                        + " [" + methodPlan.getReason() + "]");
            }
        }
        System.out.println();
    }

    private void printStaticMethods(List<MethodInfo> methods) {
        List<MethodInfo> staticMethods = methods.stream().filter(MethodInfo::isStaticMethod).collect(Collectors.toList());
        System.out.println(ConsoleStyle.heading("Static methods:"));
        if (staticMethods.isEmpty()) {
            System.out.println("  None");
        } else {
            for (MethodInfo method : staticMethods) {
                System.out.println("  * " + MethodFormatter.signature(method, verbose));
            }
        }
    }

    private void printNext(AndroidProject project, JavaClassInfo info, boolean wrapperPossible) {
        if (!wrapperPossible) {
            return;
        }
        System.out.println();
        System.out.println(ConsoleStyle.heading("Next:"));
        System.out.println("  dynamic-reflector --refactor "
                + quoteIfNeeded(project.getProjectRoot()) + " " + info.getSimpleName() + " --dry");
    }
}
