package com.dynamicreflector.cli;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.MethodInfo;
import com.dynamicreflector.spoon.SpoonClassInspector;
import com.dynamicreflector.spoon.SpoonModelContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

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
            SpoonClassInspector inspector = new SpoonClassInspector();
            Optional<JavaClassInfo> found = inspector.findClass(model, className);
            if (found.isEmpty()) {
                System.err.println("Class not found: " + className);
                return 1;
            }

            JavaClassInfo info = found.get();
            ClassClassification classification = new FileClassifier().classify(info);
            System.out.println("Class: " + info.getQualifiedName());
            System.out.println("File: " + info.getFilePath());
            System.out.println("Superclass: " + info.getSuperClassName());
            System.out.println("Classification: " + classification.getBucket());
            System.out.println("Reasons: " + String.join("; ", classification.getReasons()));
            System.out.println("Wrapper strategy possible: " + classification.isWrapperPossible());
            System.out.println();
            System.out.println("Methods found (" + info.getMethods().size() + "):");
            for (MethodInfo method : info.getMethods()) {
                System.out.println("  - " + methodLine(method));
            }
            System.out.println();
            System.out.println("Static methods:");
            info.getMethods().stream()
                    .filter(MethodInfo::isStaticMethod)
                    .forEach(method -> System.out.println("  - " + methodLine(method)));
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private String methodLine(MethodInfo method) {
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
}
