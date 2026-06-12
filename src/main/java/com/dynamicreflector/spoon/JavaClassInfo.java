package com.dynamicreflector.spoon;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class JavaClassInfo {
    private final String qualifiedName;
    private final String simpleName;
    private final Path filePath;
    private final String superClassName;
    private final List<String> interfaceNames;
    private final List<MethodInfo> methods;
    private final int fieldCount;
    private final int staticFinalFieldCount;

    public JavaClassInfo(
            String qualifiedName,
            String simpleName,
            Path filePath,
            String superClassName,
            List<String> interfaceNames,
            List<MethodInfo> methods,
            int fieldCount,
            int staticFinalFieldCount
    ) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.filePath = filePath;
        this.superClassName = superClassName;
        this.interfaceNames = Collections.unmodifiableList(interfaceNames);
        this.methods = Collections.unmodifiableList(methods);
        this.fieldCount = fieldCount;
        this.staticFinalFieldCount = staticFinalFieldCount;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getStaticFinalFieldCount() {
        return staticFinalFieldCount;
    }
}
