package com.dynamicreflector.generate;

import java.nio.file.Path;

public final class GeneratedFileConflictException extends Exception {
    public GeneratedFileConflictException(Path path) {
        super("Refusing to overwrite existing generated file with different content: " + path);
    }
}
