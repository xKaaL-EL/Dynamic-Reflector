package com.dynamicreflector.convert;

import com.dynamicreflector.spoon.MethodInfo;

public final class MethodConversionPlan {
    private final MethodInfo method;
    private final int bodyStart;
    private final int bodyEnd;
    private final String originalBody;
    private final String replacementBody;
    private final MethodConversionStatus status;

    public MethodConversionPlan(
            MethodInfo method,
            int bodyStart,
            int bodyEnd,
            String originalBody,
            String replacementBody,
            MethodConversionStatus status
    ) {
        this.method = method;
        this.bodyStart = bodyStart;
        this.bodyEnd = bodyEnd;
        this.originalBody = originalBody;
        this.replacementBody = replacementBody;
        this.status = status;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public int getBodyStart() {
        return bodyStart;
    }

    public int getBodyEnd() {
        return bodyEnd;
    }

    public String getOriginalBody() {
        return originalBody;
    }

    public String getReplacementBody() {
        return replacementBody;
    }

    public MethodConversionStatus getStatus() {
        return status;
    }
}
