package com.dynamicreflector.generate;

import java.nio.file.Path;

public final class RefactorGeneratedFile {
    private final Path path;
    private final GeneratedFileStatus status;

    public RefactorGeneratedFile(Path path, GeneratedFileStatus status) {
        this.path = path;
        this.status = status;
    }

    public Path getPath() {
        return path;
    }

    public GeneratedFileStatus getStatus() {
        return status;
    }
}
