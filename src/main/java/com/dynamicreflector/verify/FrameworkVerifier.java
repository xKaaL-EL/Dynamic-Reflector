package com.dynamicreflector.verify;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.util.JavaNameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FrameworkVerifier {
    public VerifyResult verify(AndroidProject project) {
        List<String> messages = new ArrayList<>();
        boolean success = true;

        Path javaRoot = project.getJavaSourceRoots().get(0);
        Path protectionRoot = javaRoot.resolve(JavaNameUtils.packageToPath(project.getBasePackage()))
                .resolve("protection");

        success &= check(protectionRoot.resolve("runtime").resolve("AndroidDexPluginLoader.java"), messages);
        success &= check(protectionRoot.resolve("runtime").resolve("PluginException.java"), messages);
        success &= check(protectionRoot.resolve("registry").resolve("PluginRegistry.java"), messages);
        success &= check(protectionRoot.resolve("manager").resolve("PremiumFeatureManager.java"), messages);
        success &= check(protectionRoot.resolve("config").resolve("PluginConfig.java"), messages);

        return new VerifyResult(success, messages);
    }

    private boolean check(Path path, List<String> messages) {
        if (Files.exists(path)) {
            messages.add("exists " + path);
            return true;
        }
        messages.add("missing " + path);
        return false;
    }
}
