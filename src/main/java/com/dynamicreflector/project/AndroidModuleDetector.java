package com.dynamicreflector.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public final class AndroidModuleDetector {
    public Path detectAppModule(Path projectRoot) throws ProjectDetectionException {
        Path appModule = projectRoot.resolve("app");
        if (isAndroidModule(appModule)) {
            return appModule;
        }

        if (isAndroidModule(projectRoot)) {
            return projectRoot;
        }

        try (Stream<Path> stream = Files.walk(projectRoot, 3)) {
            Optional<Path> firstModule = stream
                    .filter(Files::isDirectory)
                    .filter(this::isAndroidModule)
                    .sorted(Comparator.comparing(Path::toString))
                    .findFirst();
            if (firstModule.isPresent()) {
                return firstModule.get();
            }
        } catch (IOException e) {
            throw new ProjectDetectionException("Unable to scan project modules under: " + projectRoot, e);
        }

        throw new ProjectDetectionException("No Android app module found under: " + projectRoot);
    }

    private boolean isAndroidModule(Path path) {
        return Files.isDirectory(path)
                && Files.exists(path.resolve("src").resolve("main").resolve("AndroidManifest.xml"))
                && (Files.exists(path.resolve("build.gradle")) || Files.exists(path.resolve("build.gradle.kts")));
    }
}
