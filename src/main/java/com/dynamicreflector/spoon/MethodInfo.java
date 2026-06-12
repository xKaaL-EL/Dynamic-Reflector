package com.dynamicreflector.spoon;

import java.util.Collections;
import java.util.List;

public final class MethodInfo {
    private final String name;
    private final String returnType;
    private final List<String> parameterTypes;
    private final boolean staticMethod;
    private final boolean publicMethod;
    private final boolean protectedMethod;
    private final boolean privateMethod;
    private final boolean hasBody;

    public MethodInfo(
            String name,
            String returnType,
            List<String> parameterTypes,
            boolean staticMethod,
            boolean publicMethod,
            boolean protectedMethod,
            boolean privateMethod,
            boolean hasBody
    ) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        this.staticMethod = staticMethod;
        this.publicMethod = publicMethod;
        this.protectedMethod = protectedMethod;
        this.privateMethod = privateMethod;
        this.hasBody = hasBody;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public boolean isStaticMethod() {
        return staticMethod;
    }

    public boolean isPublicMethod() {
        return publicMethod;
    }

    public boolean isProtectedMethod() {
        return protectedMethod;
    }

    public boolean isPrivateMethod() {
        return privateMethod;
    }

    public boolean hasBody() {
        return hasBody;
    }
}
