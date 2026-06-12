package com.dynamicreflector.spoon;

import com.dynamicreflector.project.AndroidProject;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class SpoonModelLoader {
    public SpoonModelContext load(AndroidProject project) throws IOException {
        List<Path> javaFiles = collectJavaFiles(project.getJavaSourceRoots());
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(8);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setAutoImports(true);

        for (Path file : javaFiles) {
            launcher.addInputResource(file.toString());
        }

        CtModel model = launcher.buildModel();
        return new SpoonModelContext(model, javaFiles);
    }

    private List<Path> collectJavaFiles(List<Path> sourceRoots) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .filter(path -> !isExcluded(path))
                        .forEach(files::add);
            }
        }
        return files;
    }

    private boolean isExcluded(Path path) {
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = path.getFileName().toString();
        return normalized.contains("/build/")
                || normalized.contains("/.gradle/")
                || normalized.contains("/generated/")
                || normalized.contains("/res/")
                || fileName.equals("R.java")
                || fileName.equals("BuildConfig.java");
    }
}
