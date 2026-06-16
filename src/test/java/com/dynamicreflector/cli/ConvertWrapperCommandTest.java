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

final class ConvertWrapperCommandTest {
    @TempDir
    private Path tempDir;

    @Test
    void dryRunWritesNoFiles() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);
        Path source = javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java");
        String before = Files.readString(source, StandardCharsets.UTF_8);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--dry");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Dry run complete. No files were written."));
        assertTrue(result.out.contains("dynamic-reflector --convert-wrapper"));
        assertEquals(before, Files.readString(source, StandardCharsets.UTF_8));
        assertFalse(Files.exists(backupPath(project, "ScoreCalculator")));
    }

    @Test
    void applyConvertsVoidAndNonVoidMethodBodies() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(0, result.exitCode);
        String source = Files.readString(javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("import com.example.hidelauncher.protection.api.ScoreCalculatorApi;"));
        assertTrue(source.contains("import com.example.hidelauncher.protection.manager.PremiumFeatureManager;"));
        assertTrue(source.contains("return PremiumFeatureManager.get(ScoreCalculatorApi.class).doubleScore(value);"));
        assertTrue(source.contains("PremiumFeatureManager.get(ScoreCalculatorApi.class).reset(value);"));
        assertFalse(source.contains("return value * 2;"));
        assertFalse(source.contains("int copy = value;"));
        assertTrue(result.out.contains("Caller classes were not modified."));
        assertTrue(result.out.contains("Build validation is still required."));
    }

    @Test
    void applyTwiceIsIdempotentAndImportsAreAddedOnce() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);

        assertEquals(0, capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply").exitCode);
        String first = Files.readString(javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java"), StandardCharsets.UTF_8);

        CapturedResult second = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(0, second.exitCode);
        String secondSource = Files.readString(javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java"), StandardCharsets.UTF_8);
        assertEquals(first, secondSource);
        assertEquals(1, countOccurrences(secondSource, "import com.example.hidelauncher.protection.api.ScoreCalculatorApi;"));
        assertEquals(1, countOccurrences(secondSource, "import com.example.hidelauncher.protection.manager.PremiumFeatureManager;"));
        assertTrue(second.out.contains("original class already converted"));
        assertTrue(second.out.contains("methods unchanged: 2"));
    }

    @Test
    void backupFileIsCreatedFromOriginalSource() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);
        Path source = javaPath(project, "com/example/hidelauncher/logic/ScoreCalculator.java");
        String before = Files.readString(source, StandardCharsets.UTF_8);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(0, result.exitCode);
        assertTrue(Files.exists(backupPath(project, "ScoreCalculator")));
        assertEquals(before, Files.readString(backupPath(project, "ScoreCalculator"), StandardCharsets.UTF_8));
        assertTrue(result.out.contains("backup created"));
    }

    @Test
    void callerPluginConfigAndGradleFilesRemainUnchanged() throws Exception {
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
        prepareBatchThree(project);
        Path caller = javaPath(project, "com/example/hidelauncher/logic/ScoreCaller.java");
        Path pluginImpl = pluginImplPath(project, "ScoreCalculator");
        Path pluginConfig = pluginConfigPath(project);
        Path settings = project.resolve("settings.gradle");
        Path rootBuild = project.resolve("build.gradle");
        Path moduleBuild = project.resolve("app/build.gradle");
        String callerBefore = Files.readString(caller, StandardCharsets.UTF_8);
        String pluginImplBefore = Files.readString(pluginImpl, StandardCharsets.UTF_8);
        String pluginConfigBefore = Files.readString(pluginConfig, StandardCharsets.UTF_8);
        String settingsBefore = Files.readString(settings, StandardCharsets.UTF_8);
        String rootBuildBefore = Files.readString(rootBuild, StandardCharsets.UTF_8);
        String moduleBuildBefore = Files.readString(moduleBuild, StandardCharsets.UTF_8);

        assertEquals(0, capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply").exitCode);

        assertEquals(callerBefore, Files.readString(caller, StandardCharsets.UTF_8));
        assertEquals(pluginImplBefore, Files.readString(pluginImpl, StandardCharsets.UTF_8));
        assertEquals(pluginConfigBefore, Files.readString(pluginConfig, StandardCharsets.UTF_8));
        assertEquals(settingsBefore, Files.readString(settings, StandardCharsets.UTF_8));
        assertEquals(rootBuildBefore, Files.readString(rootBuild, StandardCharsets.UTF_8));
        assertEquals(moduleBuildBefore, Files.readString(moduleBuild, StandardCharsets.UTF_8));
    }

    @Test
    void missingRuntimePrintsInitCommand() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Runtime framework missing."));
        assertTrue(result.err.contains("dynamic-reflector --init"));
    }

    @Test
    void missingApiOrWrapperPrintsRefactorCommand() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("API missing."));
        assertTrue(result.err.contains("dynamic-reflector --refactor"));
        assertTrue(result.err.contains("ScoreCalculator --apply"));
    }

    @Test
    void missingPluginArtifactsPrintsGeneratePluginCommand() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "ScoreCalculator", "--apply").exitCode);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Plugin implementation missing."));
        assertTrue(result.err.contains("dynamic-reflector --generate-plugin"));
    }

    @Test
    void statefulClassUsingFieldsIsRejected() throws Exception {
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
        satisfyPluginPreconditions(project, "StatefulCalculator");

        CapturedResult result = capture("--convert-wrapper", project.toString(), "StatefulCalculator", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Original class delegation conversion is not safe for this class in Batch 4."));
        assertTrue(result.err.contains("uses instance fields/state"));
    }

    @Test
    void androidComponentClassIsRejected() throws Exception {
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

        CapturedResult result = capture("--convert-wrapper", project.toString(), "PremiumService", "--apply");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Class is not a safe Batch 2 candidate: REVIEW_REQUIRED"));
    }

    @Test
    void featureVerifyReportsConvertedStatus() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);
        assertEquals(0, capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply").exitCode);

        CapturedResult result = capture("--verify", project.toString(), "ScoreCalculator");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Original class delegation: converted"));
    }

    @Test
    void existingPublicCommandsStillWork() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);

        assertEquals(0, capture("--help").exitCode);
        assertEquals(0, capture("--examples").exitCode);
        assertEquals(0, capture("--analyse", project.toString()).exitCode);
        assertEquals(0, capture("--inspect", project.toString(), "ScoreCalculator").exitCode);
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "ScoreCalculator", "--apply").exitCode);
        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);
        assertEquals(0, capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--apply").exitCode);
        assertEquals(0, capture("--verify", project.toString(), "ScoreCalculator").exitCode);
    }

    @Test
    void convertWrapperOutputEndsWithFinalBlankLine() throws Exception {
        Path project = createAndroidProject();
        writeScoreCalculator(project);
        prepareBatchThree(project);

        CapturedResult result = capture("--convert-wrapper", project.toString(), "ScoreCalculator", "--dry");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.endsWith(System.lineSeparator() + System.lineSeparator()));
    }

    private void prepareBatchThree(Path project) throws Exception {
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "ScoreCalculator", "--apply").exitCode);
        assertEquals(0, capture("--generate-plugin", project.toString(), "ScoreCalculator", "--apply").exitCode);
    }

    private void satisfyPluginPreconditions(Path project, String className) throws Exception {
        Path implementation = pluginImplPath(project, className);
        Files.createDirectories(implementation.getParent());
        Files.writeString(
                implementation,
                """
                        package com.example.hidelauncher.premium.impl;

                        import com.example.hidelauncher.protection.api.%sApi;

                        public final class %sImpl implements %sApi {
                            public int update(int value) {
                                return value;
                            }
                        }
                        """.formatted(className, className, className),
                StandardCharsets.UTF_8
        );
        Path pluginConfig = pluginConfigPath(project);
        String content = Files.readString(pluginConfig, StandardCharsets.UTF_8)
                .replace(
                        "import com.example.hidelauncher.protection.registry.PluginRegistry;",
                        "import com.example.hidelauncher.protection.registry.PluginRegistry;\n"
                                + "import com.example.hidelauncher.protection.api." + className + "Api;"
                )
                .replace(
                        "        // The CLI will add one mapping here per approved protected class.",
                        "        registry.register(" + className + "Api.class, \"com.example.hidelauncher.premium.impl."
                                + className + "Impl\", \"" + className.toLowerCase() + "\");\n"
                                + "        // The CLI will add one mapping here per approved protected class."
                );
        Files.writeString(pluginConfig, content, StandardCharsets.UTF_8);
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
        Path project = tempDir.resolve("android-app-" + System.nanoTime());
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

                            public void reset(int value) {
                                int copy = value;
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

    private Path backupPath(Path project, String className) {
        return javaPath(project, "com/example/hidelauncher/logic/" + className + ".java.bak-dynamic-reflector");
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
