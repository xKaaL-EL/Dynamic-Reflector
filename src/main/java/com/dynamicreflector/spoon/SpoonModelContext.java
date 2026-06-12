package com.dynamicreflector.spoon;

import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class SpoonModelContext {
    private final CtModel model;
    private final List<Path> sourceFiles;

    public SpoonModelContext(CtModel model, List<Path> sourceFiles) {
        this.model = model;
        this.sourceFiles = Collections.unmodifiableList(sourceFiles);
    }

    public CtModel getModel() {
        return model;
    }

    public List<Path> getSourceFiles() {
        return sourceFiles;
    }
}
