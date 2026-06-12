package com.dynamicreflector.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SourceSetDetector {
    public List<Path> detectJavaSourceRoots(Path moduleRoot) {
        List<Path> roots = new ArrayList<>();
        Path mainJava = moduleRoot.resolve("src").resolve("main").resolve("java");
        if (Files.isDirectory(mainJava)) {
            roots.add(mainJava);
        }
        return roots;
    }
}
