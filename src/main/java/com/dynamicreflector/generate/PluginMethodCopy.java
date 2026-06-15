package com.dynamicreflector.generate;

import com.dynamicreflector.spoon.MethodInfo;

public final class PluginMethodCopy {
    private final MethodInfo method;
    private final String body;

    public PluginMethodCopy(MethodInfo method, String body) {
        this.method = method;
        this.body = body;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }
}
