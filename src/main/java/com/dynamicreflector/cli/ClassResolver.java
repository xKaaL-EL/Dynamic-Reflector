package com.dynamicreflector.cli;

import com.dynamicreflector.classify.ClassificationBucket;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonClassInspector;
import com.dynamicreflector.spoon.SpoonModelContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ClassResolver {
    private final FileClassifier classifier = new FileClassifier();

    public JavaClassInfo resolve(SpoonModelContext model, String requestedClass) throws ClassResolutionException {
        List<JavaClassInfo> classes = new SpoonClassInspector().listClasses(model)
                .stream()
                .sorted(Comparator.comparing(JavaClassInfo::getQualifiedName))
                .collect(Collectors.toList());

        String normalized = requestedClass.trim();
        List<JavaClassInfo> exact = classes.stream()
                .filter(info -> info.getQualifiedName().equals(normalized))
                .collect(Collectors.toList());
        if (exact.size() == 1) {
            return exact.get(0);
        }

        List<JavaClassInfo> matches = classes.stream()
                .filter(info -> info.getSimpleName().equals(normalized)
                        || info.getQualifiedName().endsWith("." + normalized))
                .collect(Collectors.toList());
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.size() > 1) {
            throw new ClassResolutionException("Duplicate class name '" + requestedClass
                    + "'. Use a fully-qualified class name. Matches:\n" + formatClasses(matches));
        }

        List<JavaClassInfo> candidates = classes.stream()
                .filter(info -> classifier.classify(info).getBucket() == ClassificationBucket.CANDIDATE)
                .collect(Collectors.toList());
        throw new ClassResolutionException("Class not found: " + requestedClass
                + "\nAvailable candidate classes:\n" + formatClasses(candidates));
    }

    private String formatClasses(List<JavaClassInfo> classes) {
        if (classes.isEmpty()) {
            return "  - none";
        }
        return classes.stream()
                .map(info -> "  - " + info.getQualifiedName())
                .collect(Collectors.joining("\n"));
    }
}
