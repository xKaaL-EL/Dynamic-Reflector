package com.dynamicreflector.spoon;

import java.util.Collections;
import java.util.List;

public final class MethodInfo {
    private final String name;
    private final String returnType;
    private final List<String> parameterNames;
    private final List<String> parameterTypes;
    private final List<String> thrownTypes;
    private final boolean staticMethod;
    private final boolean publicMethod;
    private final boolean protectedMethod;
    private final boolean privateMethod;
    private final boolean abstractMethod;
    private final boolean nativeMethod;
    private final boolean varArgs;
    private final boolean hasBody;
    private final int typeParameterCount;

    public MethodInfo(
            String name,
            String returnType,
            List<String> parameterNames,
            List<String> parameterTypes,
            List<String> thrownTypes,
            boolean staticMethod,
            boolean publicMethod,
            boolean protectedMethod,
            boolean privateMethod,
            boolean abstractMethod,
            boolean nativeMethod,
            boolean varArgs,
            int typeParameterCount,
            boolean hasBody
    ) {
        this.name = name;
        this.returnType = returnType;
        this.parameterNames = Collections.unmodifiableList(parameterNames);
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        this.thrownTypes = Collections.unmodifiableList(thrownTypes);
        this.staticMethod = staticMethod;
        this.publicMethod = publicMethod;
        this.protectedMethod = protectedMethod;
        this.privateMethod = privateMethod;
        this.abstractMethod = abstractMethod;
        this.nativeMethod = nativeMethod;
        this.varArgs = varArgs;
        this.typeParameterCount = typeParameterCount;
        this.hasBody = hasBody;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public List<String> getThrownTypes() {
        return thrownTypes;
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

    public boolean isAbstractMethod() {
        return abstractMethod;
    }

    public boolean isNativeMethod() {
        return nativeMethod;
    }

    public boolean isVarArgs() {
        return varArgs;
    }

    public int getTypeParameterCount() {
        return typeParameterCount;
    }

    public boolean hasBody() {
        return hasBody;
    }
}
