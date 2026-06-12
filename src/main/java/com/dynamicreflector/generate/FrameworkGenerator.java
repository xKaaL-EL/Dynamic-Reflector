package com.dynamicreflector.generate;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.util.JavaNameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FrameworkGenerator {
    public List<GeneratedFile> generateMinimalFramework(AndroidProject project) throws IOException {
        String basePackage = project.getBasePackage();
        Path javaRoot = project.getJavaSourceRoots().get(0);
        Path protectionRoot = javaRoot.resolve(JavaNameUtils.packageToPath(basePackage))
                .resolve("protection");

        List<GeneratedFile> files = new ArrayList<>();
        files.add(writeIfMissing(
                protectionRoot.resolve("runtime").resolve("AndroidDexPluginLoader.java"),
                androidDexPluginLoader(basePackage)
        ));
        files.add(writeIfMissing(
                protectionRoot.resolve("runtime").resolve("PluginException.java"),
                pluginException(basePackage)
        ));
        files.add(writeIfMissing(
                protectionRoot.resolve("registry").resolve("PluginRegistry.java"),
                pluginRegistry(basePackage)
        ));
        files.add(writeIfMissing(
                protectionRoot.resolve("manager").resolve("PremiumFeatureManager.java"),
                premiumFeatureManager(basePackage)
        ));
        files.add(writeIfMissing(
                protectionRoot.resolve("config").resolve("PluginConfig.java"),
                pluginConfig(basePackage)
        ));
        return files;
    }

    private GeneratedFile writeIfMissing(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            return new GeneratedFile(path, false);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return new GeneratedFile(path, true);
    }

    private String androidDexPluginLoader(String basePackage) {
        return """
                package %s.protection.runtime;

                import android.content.Context;

                import java.io.File;
                import java.io.FileInputStream;
                import java.security.MessageDigest;

                import dalvik.system.DexClassLoader;

                import %s.protection.config.PluginConfig;

                public final class AndroidDexPluginLoader {
                    private final Context appContext;
                    private volatile ClassLoader classLoader;

                    public AndroidDexPluginLoader(Context context) {
                        this.appContext = context.getApplicationContext();
                    }

                    public synchronized ClassLoader getClassLoader() {
                        if (classLoader == null) {
                            classLoader = createClassLoader();
                        }
                        return classLoader;
                    }

                    private ClassLoader createClassLoader() {
                        File pluginApk = new File(PluginConfig.getPluginApkPath(appContext));
                        if (!pluginApk.isFile()) {
                            throw new PluginException("Plugin APK not found: " + pluginApk.getAbsolutePath());
                        }

                        verifySha256IfConfigured(pluginApk);

                        File optimizedDir = new File(appContext.getCodeCacheDir(), "premium-plugin-optimized");
                        if (!optimizedDir.exists() && !optimizedDir.mkdirs()) {
                            throw new PluginException("Unable to create optimized dex directory: " + optimizedDir.getAbsolutePath());
                        }

                        return new DexClassLoader(
                                pluginApk.getAbsolutePath(),
                                optimizedDir.getAbsolutePath(),
                                null,
                                appContext.getClassLoader()
                        );
                    }

                    private void verifySha256IfConfigured(File pluginApk) {
                        String expected = PluginConfig.getExpectedSha256();
                        if (expected == null || expected.trim().isEmpty()) {
                            return;
                        }

                        String actual = sha256(pluginApk);
                        if (!expected.equalsIgnoreCase(actual)) {
                            throw new PluginException("Plugin SHA-256 mismatch.");
                        }
                    }

                    private String sha256(File file) {
                        try (FileInputStream input = new FileInputStream(file)) {
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = input.read(buffer)) != -1) {
                                digest.update(buffer, 0, read);
                            }
                            byte[] hash = digest.digest();
                            StringBuilder builder = new StringBuilder(hash.length * 2);
                            for (byte value : hash) {
                                builder.append(String.format("%%02x", value));
                            }
                            return builder.toString();
                        } catch (Exception e) {
                            throw new PluginException("Unable to calculate plugin SHA-256.", e);
                        }
                    }
                }
                """.formatted(basePackage, basePackage);
    }

    private String pluginException(String basePackage) {
        return """
                package %s.protection.runtime;

                public final class PluginException extends RuntimeException {
                    public PluginException(String message) {
                        super(message);
                    }

                    public PluginException(String message, Throwable cause) {
                        super(message, cause);
                    }
                }
                """.formatted(basePackage);
    }

    private String pluginRegistry(String basePackage) {
        return """
                package %s.protection.registry;

                import android.content.Context;

                import java.lang.reflect.Constructor;
                import java.util.HashMap;
                import java.util.LinkedHashMap;
                import java.util.List;
                import java.util.Map;

                import %s.protection.config.PluginConfig;
                import %s.protection.runtime.AndroidDexPluginLoader;
                import %s.protection.runtime.PluginException;

                public final class PluginRegistry {
                    private final AndroidDexPluginLoader loader;
                    private final Map<Class<?>, Mapping> mappings = new LinkedHashMap<>();
                    private final Map<Class<?>, Object> instances = new HashMap<>();

                    public PluginRegistry(Context context) {
                        this.loader = new AndroidDexPluginLoader(context);
                    }

                    public <T> void register(Class<T> apiType, String implementationClassName, String featureName) {
                        if (apiType == null) {
                            throw new PluginException("API type cannot be null.");
                        }
                        if (implementationClassName == null || implementationClassName.trim().isEmpty()) {
                            throw new PluginException("Implementation class cannot be empty for API: " + apiType.getName());
                        }
                        ensureAllowedPackage(implementationClassName);
                        mappings.put(apiType, new Mapping(implementationClassName, featureName));
                    }

                    public synchronized <T> T get(Class<T> apiType) {
                        Mapping mapping = mappings.get(apiType);
                        if (mapping == null) {
                            throw new PluginException("No plugin mapping registered for API: " + apiType.getName());
                        }

                        Object cached = instances.get(apiType);
                        if (cached != null) {
                            return apiType.cast(cached);
                        }

                        Object created = createInstance(apiType, mapping);
                        instances.put(apiType, created);
                        return apiType.cast(created);
                    }

                    private <T> Object createInstance(Class<T> apiType, Mapping mapping) {
                        try {
                            Class<?> implementationClass = loader.getClassLoader().loadClass(mapping.implementationClassName);
                            if (!apiType.isAssignableFrom(implementationClass)) {
                                throw new PluginException("Implementation does not implement API: " + mapping.implementationClassName);
                            }
                            Constructor<?> constructor = implementationClass.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            return constructor.newInstance();
                        } catch (PluginException e) {
                            throw e;
                        } catch (ClassNotFoundException e) {
                            throw new PluginException("Plugin implementation class not found: " + mapping.implementationClassName, e);
                        } catch (NoSuchMethodException e) {
                            throw new PluginException("Plugin implementation requires a no-arg constructor: " + mapping.implementationClassName, e);
                        } catch (Exception e) {
                            throw new PluginException("Unable to instantiate plugin implementation: " + mapping.implementationClassName, e);
                        }
                    }

                    private void ensureAllowedPackage(String implementationClassName) {
                        List<String> allowedPrefixes = PluginConfig.getAllowedPackagePrefixes();
                        for (String prefix : allowedPrefixes) {
                            if (implementationClassName.startsWith(prefix)) {
                                return;
                            }
                        }
                        throw new PluginException("Implementation package is not allowlisted: " + implementationClassName);
                    }

                    private static final class Mapping {
                        private final String implementationClassName;
                        private final String featureName;

                        private Mapping(String implementationClassName, String featureName) {
                            this.implementationClassName = implementationClassName;
                            this.featureName = featureName;
                        }
                    }
                }
                """.formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String premiumFeatureManager(String basePackage) {
        return """
                package %s.protection.manager;

                import android.content.Context;

                import %s.protection.config.PluginConfig;
                import %s.protection.registry.PluginRegistry;
                import %s.protection.runtime.PluginException;

                public final class PremiumFeatureManager {
                    private static volatile PluginRegistry registry;

                    private PremiumFeatureManager() {
                    }

                    public static synchronized void initialize(Context context) {
                        if (registry != null) {
                            return;
                        }
                        PluginRegistry created = new PluginRegistry(context.getApplicationContext());
                        PluginConfig.registerMappings(created);
                        registry = created;
                    }

                    public static <T> T get(Class<T> apiType) {
                        PluginRegistry current = registry;
                        if (current == null) {
                            throw new PluginException("PremiumFeatureManager is not initialized. Call initialize(context) first.");
                        }
                        return current.get(apiType);
                    }

                    static boolean isFeatureEnabled(String featureName) {
                        return true;
                    }
                }
                """.formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String pluginConfig(String basePackage) {
        return """
                package %s.protection.config;

                import android.content.Context;

                import java.io.File;
                import java.util.Arrays;
                import java.util.List;

                import %s.protection.registry.PluginRegistry;

                public final class PluginConfig {
                    private PluginConfig() {
                    }

                    public static String getPluginApkPath(Context context) {
                        return new File(context.getFilesDir(), "plugins/premium-plugin.apk").getAbsolutePath();
                    }

                    public static String getExpectedSha256() {
                        return "";
                    }

                    public static List<String> getAllowedPackagePrefixes() {
                        return Arrays.asList("%s.premium.impl");
                    }

                    public static void registerMappings(PluginRegistry registry) {
                        // The CLI will add one mapping here per approved protected class.
                    }
                }
                """.formatted(basePackage, basePackage, basePackage);
    }
}
