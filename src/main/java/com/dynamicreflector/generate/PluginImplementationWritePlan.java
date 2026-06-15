package com.dynamicreflector.generate;

import java.nio.file.Path;

public final class PluginImplementationWritePlan {
    private final Path path;
    private final String content;
    private final GeneratedFileStatus status;

    public PluginImplementationWritePlan(Path path, String content, GeneratedFileStatus status) {
        this.path = path;
        this.content = content;
        this.status = status;
    }

    public Path getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public GeneratedFileStatus getStatus() {
        return status;
    }
}
