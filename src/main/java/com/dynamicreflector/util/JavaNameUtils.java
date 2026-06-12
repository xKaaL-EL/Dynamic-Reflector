package com.dynamicreflector.util;

import java.nio.file.Path;

public final class JavaNameUtils {
    private JavaNameUtils() {
    }

    public static Path packageToPath(String packageName) {
        return Path.of(packageName.replace('.', '/'));
    }
}
