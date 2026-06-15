package com.dynamicreflector.cli;

import com.dynamicreflector.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneratePluginCommandTest {
    @TempDir
    private Path tempDir;

    @Test
    void dryRunWritesNoFiles() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--dry");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Dry run complete. No files were written."));
        assertTrue(result.out.contains("dynamic-reflector --generate-plugin"));
        assertFalse(Files.exists(pluginImplPath(project, "ScoreCalculator")));
        assertFalse(readPluginConfig(project).contains("ScoreCalculatorApi.class"));
    }

    @Test
    void applyGeneratesImplementationForStatelessHelper() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(0, result.exitCode);
        Path implementation = pluginImplPath(project, "ScoreCalculator");
        assertTrue(Files.exists(implementation));
        String content = Files.readString(implementation, StandardCharsets.UTF_8);
        assertTrue(content.contains("package com.example.hidelauncher.premium.impl;"));
        assertTrue(content.contains("import com.example.hidelauncher.protection.api.ScoreCalculatorApi;"));
        assertTrue(content.contains("public final class ScoreCalculatorImpl implements ScoreCalculatorApi"));
        assertTrue(content.contains("return value * 2;"));
        assertTrue(content.contains("return value + 10;"));
        assertTrue(result.out.contains("Original class was not modified."));
        assertTrue(result.out.contains("Wrapper conversion is planned for Batch 4."));
    }

    @Test
    void pluginConfigMappingIsAddedOnceAndApplyIsIdempotent() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);

        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);
        String firstConfig = readPluginConfig(project);
        String firstImpl = Files.readString(pluginImplPath(project, "ScoreCalculator"), StandardCharsets.UTF_8);

        CapturedResult second = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(0, second.exitCode);
        assertEquals(firstConfig, readPluginConfig(project));
        assertEquals(firstImpl, Files.readString(pluginImplPath(project, "ScoreCalculator"), StandardCharsets.UTF_8));
        assertEquals(1, countOccurrences(readPluginConfig(project), "registry.register(ScoreCalculatorApi.class"));
        assertTrue(second.out.contains("PluginConfig mapping unchanged"));
        assertTrue(second.out.contains("allowed prefix unchanged"));
    }

    @Test
    void conflictingMappingRefusesOverwriteAndWritesNoImplementation() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);
        Path pluginConfig = pluginConfigPath(project);
        String conflict = readPluginConfig(project).replace(
                "        // The CLI will add one mapping here per approved protected class.",
                "        registry.register(ScoreCalculatorApi.class, \"wrong.impl.ScoreCalculatorImpl\", \"score_calculator\");\n"
                        + "        // The CLI will add one mapping here per approved protected class."
        ).replace(
                "import com.example.hidelauncher.protection.registry.PluginRegistry;",
                "import com.example.hidelauncher.protection.registry.PluginRegistry;\n"
                        + "import com.example.hidelauncher.protection.api.ScoreCalculatorApi;"
        );
        Files.writeString(pluginConfig, conflict, StandardCharsets.UTF_8);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Conflicting PluginConfig mapping"));
        assertFalse(Files.exists(pluginImplPath(project, "ScoreCalculator")));
    }

    @Test
    void missingApiPrintsRefactorCommand() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("API not found."));
        assertTrue(result.err.contains("dynamic-reflector --refactor"));
        assertTrue(result.err.contains("ScoreCalculator --apply"));
    }

    @Test
    void missingFrameworkPrintsInitCommand() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Runtime framework missing."));
        assertTrue(result.err.contains("dynamic-reflector --init"));
    }

    @Test
    void statefulMethodUsingFieldsIsRejected() throws Exception {
        Path project = createAndroidProject();
        writeJava(
                project,
                "com/example/hidelauncher/logic/StatefulCalculator.java",
                """
                        package com.example.hidelauncher.logic;

                        public class StatefulCalculator {
                            private int total;

                            public int update(int value) {
                                total += value;
                                return total;
                            }
                        }
                        """
        );
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "StatefulCalculator", "--apply").exitCode);

        CapturedResult result = capture("--generate-plugin", project.toString(), "StatefulCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Plugin implementation generation is not safe for this class in Batch 3."));
        assertTrue(result.err.contains("uses instance fields/state"));
        assertFalse(Files.exists(pluginImplPath(project, "StatefulCalculator")));
    }

    @Test
    void androidLifecycleClassesRemainRejected() throws Exception {
        Path project = createAndroidProject();
        writeJava(
                project,
                "com/example/hidelauncher/PremiumService.java",
                """
                        package com.example.hidelauncher;

                        import android.app.Service;

                        public class PremiumService extends Service {
                            public int calculate(int value) {
                                return value * 2;
                            }
                        }
                        """
        );

        CapturedResult result = capture("--generate-plugin", project.toString(), "PremiumService", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Class is not a safe Batch 2 candidate: REVIEW_REQUIRED"));
    }

    @Test
    void originalCallerAndGradleFilesRemainUnchanged() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        writeJava(
                project,
                "com/example/hidelauncher/logic/ScoreCaller.java",
                """
                        package com.example.hidelauncher.logic;

                        public class ScoreCaller {
                            public int call(int value) {
                                return new ScoreCalculator().doubleScore(value);
                            }
                        }
                        """
        );
        prepareFrameworkAndApi(project);
        Path original = javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java");
        Path caller = javaPath(project, "com/example/hidelauncher/logic/ScoreCaller.java");
        Path settings = project.resolve("settings.gradle");
        Path rootBuild = project.resolve("build.gradle");
        Path moduleBuild = project.resolve("app/build.gradle");
        String originalBefore = Files.readString(original, StandardCharsets.UTF_8);
        String callerBefore = Files.readString(caller, StandardCharsets.UTF_8);
        String settingsBefore = Files.readString(settings, StandardCharsets.UTF_8);
        String rootBuildBefore = Files.readString(rootBuild, StandardCharsets.UTF_8);
        String moduleBuildBefore = Files.readString(moduleBuild, StandardCharsets.UTF_8);

        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);

        assertEquals(originalBefore, Files.readString(original, StandardCharsets.UTF_8));
        assertEquals(callerBefore, Files.readString(caller, StandardCharsets.UTF_8));
        assertEquals(settingsBefore, Files.readString(settings, StandardCharsets.UTF_8));
        assertEquals(rootBuildBefore, Files.readString(rootBuild, StandardCharsets.UTF_8));
        assertEquals(moduleBuildBefore, Files.readString(moduleBuild, StandardCharsets.UTF_8));
    }

    @Test
    void featureVerifyChecksBatchThreeArtifacts() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);
        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);

        CapturedResult result = capture("--verify", project.toString(), "ScoreCalculator");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Feature Verification"));
        assertTrue(result.out.contains("API: found"));
        assertTrue(result.out.contains("Wrapper: found"));
        assertTrue(result.out.contains("Plugin implementation: found"));
        assertTrue(result.out.contains("PluginConfig mapping: found"));
        assertTrue(result.out.contains("Allowed implementation prefix: found"));
        assertTrue(result.out.contains("Wrapper conversion: pending Batch 4"));
    }

    @Test
    void protectionClassesRemainExcludedAfterPluginGeneration() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);
        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);

        CapturedResult result = capture("--analyse", project.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Safe candidates (1):"));
        assertTrue(result.out.contains("* ScoreCalculator"));
        assertFalse(result.out.contains("ScoreCalculatorApi"));
        assertFalse(result.out.contains("ScoreCalculatorWrapper"));
        assertFalse(result.out.contains("PluginConfig"));
    }

    @Test
    void generatePluginOutputEndsWithFinalBlankLine() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareFrameworkAndApi(project);

        CapturedResult result = capture("--generate-plugin", project.toString(), "ScoreCalculator", "--dry");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.endsWith(System.lineSeparator() + System.lineSeparator()));
    }

    private void prepareFrameworkAndApi(Path project) throws Exception {
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "ScoreCalculator", "--apply").exitCode);
    }

    private CapturedResult capture(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            int exitCode = Main.execute(args);
            return new CapturedResult(
                    exitCode,
                    out.toString(StandardCharsets.UTF_8),
                    err.toString(StandardCharsets.UTF_8)
            );
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private Path createAndroidProject() throws Exception {
        Path project = tempDir.resolve("android-app");
        Files.createDirectories(project);
        Files.writeString(project.resolve("settings.gradle"), "include ':app'\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), "", StandardCharsets.UTF_8);
        Path app = project.resolve("app");
        Files.createDirectories(app);
        Files.writeString(
                app.resolve("build.gradle"),
                """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            namespace 'com.example.hidelauncher'
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Path manifest = app.resolve("src/main/AndroidManifest.xml");
        Files.createDirectories(manifest.getParent());
        Files.writeString(
                manifest,
                """
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                            <application />
                        </manifest>
                        """,
                StandardCharsets.UTF_8
        );
        Files.createDirectories(app.resolve("src/main/java"));
        return project;
    }

    private void writeScoreCalculator(Path project) throws Exception {
        writeJava(
                project,
                "com/example/hidelauncher/logic/ScoreCalculator.java",
                """
                        package com.example.hidelauncher.logic;

                        public class ScoreCalculator {
                            public int doubleScore(int value) {
                                return value * 2;
                            }

                            public int bonusScore(int value) {
                                return value + 10;
                            }
                        }
                        """
        );
    }

    private void writeJava(Path project, String relativePath, String content) throws Exception {
        Path file = javaPath(project, relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private Path javaPath(Path project, String relativePath) {
        return project.resolve("app/src/main/java").resolve(relativePath);
    }

    private Path pluginImplPath(Path project, String className) {
        return project.resolve("premium-plugin/src/main/java/com/example/hidelauncher/premium/impl")
                .resolve(className + "Impl.java");
    }

    private Path pluginConfigPath(Path project) {
        return project.resolve("app/src/main/java/com/example/hidelauncher/protection/config/PluginConfig.java");
    }

    private String readPluginConfig(Path project) throws Exception {
        return Files.readString(pluginConfigPath(project), StandardCharsets.UTF_8);
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private record CapturedResult(int exitCode, String out, String err) {
    }
}
