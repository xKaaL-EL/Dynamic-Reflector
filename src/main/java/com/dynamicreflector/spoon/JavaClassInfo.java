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
    private final boolean interfaceType;
    private final boolean abstractType;
    private final boolean enumType;
    private final boolean annotationType;

    public JavaClassInfo(
            String qualifiedName,
            String simpleName,
            Path filePath,
            String superClassName,
            List<String> interfaceNames,
            List<MethodInfo> methods,
            int fieldCount,
            int staticFinalFieldCount,
            boolean interfaceType,
            boolean abstractType,
            boolean enumType,
            boolean annotationType
    ) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.filePath = filePath;
        this.superClassName = superClassName;
        this.interfaceNames = Collections.unmodifiableList(interfaceNames);
        this.methods = Collections.unmodifiableList(methods);
        this.fieldCount = fieldCount;
        this.staticFinalFieldCount = staticFinalFieldCount;
        this.interfaceType = interfaceType;
        this.abstractType = abstractType;
        this.enumType = enumType;
        this.annotationType = annotationType;
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

    public boolean isInterfaceType() {
        return interfaceType;
    }

    public boolean isAbstractType() {
        return abstractType;
    }

    public boolean isEnumType() {
        return enumType;
    }

    public boolean isAnnotationType() {
        return annotationType;
    }
}
