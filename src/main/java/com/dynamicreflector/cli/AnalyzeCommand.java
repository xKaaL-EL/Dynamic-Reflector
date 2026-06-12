package com.dynamicreflector.cli;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.classify.ClassificationBucket;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonClassInspector;
import com.dynamicreflector.spoon.SpoonModelContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "analyze", description = "Print a console-only summary of an Android Java project.")
public final class AnalyzeCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Override
    public Integer call() {
        try {
            AndroidProject project = locateProject(projectPath, basePackage);
            SpoonModelContext model = loadModel(project);
            List<JavaClassInfo> classes = new SpoonClassInspector().listClasses(model);
            FileClassifier classifier = new FileClassifier();

            Map<ClassificationBucket, List<JavaClassInfo>> grouped = classes.stream()
                    .collect(Collectors.groupingBy(
                            info -> classifier.classify(info).getBucket(),
                            () -> new EnumMap<>(ClassificationBucket.class),
                            Collectors.toList()
                    ));

            printProject(project);
            System.out.println();
            System.out.println("Java files scanned: " + model.getSourceFiles().size());
            System.out.println("Classes found: " + classes.size());
            printBucket("Hard excluded", ClassificationBucket.HARD_EXCLUDED, grouped, classifier);
            printBucket("Candidate", ClassificationBucket.CANDIDATE, grouped, classifier);
            printBucket("Review required", ClassificationBucket.REVIEW_REQUIRED, grouped, classifier);
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private void printBucket(
            String title,
            ClassificationBucket bucket,
            Map<ClassificationBucket, List<JavaClassInfo>> grouped,
            FileClassifier classifier
    ) {
        List<JavaClassInfo> items = grouped.getOrDefault(bucket, List.of());
        System.out.println();
        System.out.println(title + " (" + items.size() + "):");
        for (JavaClassInfo info : items) {
            ClassClassification classification = classifier.classify(info);
            System.out.println("  - " + info.getQualifiedName());
            System.out.println("    " + String.join("; ", classification.getReasons()));
        }
    }
}
