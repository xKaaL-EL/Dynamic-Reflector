package com.dynamicreflector.refactor;

import com.dynamicreflector.classify.ClassClassification;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.spoon.JavaClassInfo;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class RefactorPlan {
    private final AndroidProject project;
    private final JavaClassInfo classInfo;
    private final ClassClassification classification;
    private final String apiPackage;
    private final String wrapperPackage;
    private final String apiName;
    private final String wrapperName;
    private final Path apiPath;
    private final Path wrapperPath;
    private final List<MethodPlan> methods;

    public RefactorPlan(
            AndroidProject project,
            JavaClassInfo classInfo,
            ClassClassification classification,
            String apiPackage,
            String wrapperPackage,
            String apiName,
            String wrapperName,
            Path apiPath,
            Path wrapperPath,
            List<MethodPlan> methods
    ) {
        this.project = project;
        this.classInfo = classInfo;
        this.classification = classification;
        this.apiPackage = apiPackage;
        this.wrapperPackage = wrapperPackage;
        this.apiName = apiName;
        this.wrapperName = wrapperName;
        this.apiPath = apiPath;
        this.wrapperPath = wrapperPath;
        this.methods = Collections.unmodifiableList(methods);
    }

    public AndroidProject getProject() {
        return project;
    }

    public JavaClassInfo getClassInfo() {
        return classInfo;
    }

    public ClassClassification getClassification() {
        return classification;
    }

    public String getApiPackage() {
        return apiPackage;
    }

    public String getWrapperPackage() {
        return wrapperPackage;
    }

    public String getApiName() {
        return apiName;
    }

    public String getWrapperName() {
        return wrapperName;
    }

    public Path getApiPath() {
        return apiPath;
    }

    public Path getWrapperPath() {
        return wrapperPath;
    }

    public List<MethodPlan> getMethods() {
        return methods;
    }

    public List<MethodPlan> getIncludedMethods() {
        return methods.stream().filter(MethodPlan::isIncluded).collect(Collectors.toList());
    }

    public List<MethodPlan> getSkippedMethods() {
        return methods.stream().filter(method -> !method.isIncluded()).collect(Collectors.toList());
    }
}
