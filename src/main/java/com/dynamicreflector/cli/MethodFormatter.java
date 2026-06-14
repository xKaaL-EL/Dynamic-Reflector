package com.dynamicreflector.cli;

import com.dynamicreflector.spoon.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public final class MethodFormatter {
    private MethodFormatter() {
    }

    public static String signature(MethodInfo method, boolean verbose) {
        return displayType(method.getReturnType(), verbose)
                + " "
                + method.getName()
                + "(" + parameters(method, verbose) + ")";
    }

    private static String parameters(MethodInfo method, boolean verbose) {
        List<String> parameters = new ArrayList<>();
        for (String type : method.getParameterTypes()) {
            parameters.add(displayType(type, verbose));
        }
        return String.join(", ", parameters);
    }

    public static String displayType(String typeName, boolean verbose) {
        if (verbose || typeName == null) {
            return typeName;
        }
        String suffix = "";
        String type = typeName;
        while (type.endsWith("[]")) {
            suffix += "[]";
            type = type.substring(0, type.length() - 2);
        }
        int lastDot = type.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < type.length() - 1) {
            type = type.substring(lastDot + 1);
        }
        return type + suffix;
    }
}
