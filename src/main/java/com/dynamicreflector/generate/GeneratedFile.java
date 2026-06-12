package com.dynamicreflector.generate;

import java.nio.file.Path;

public final class GeneratedFile {
    private final Path path;
    private final boolean created;

    public GeneratedFile(Path path, boolean created) {
        this.path = path;
        this.created = created;
    }

    public Path getPath() {
        return path;
    }

    public boolean isCreated() {
        return created;
    }
}
