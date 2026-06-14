package com.dynamicreflector.cli;

import com.dynamicreflector.Main;
import com.dynamicreflector.classify.ClassificationBucket;
import com.dynamicreflector.classify.FileClassifier;
import com.dynamicreflector.spoon.JavaClassInfo;
import com.dynamicreflector.spoon.SpoonClassInspector;
import com.dynamicreflector.spoon.SpoonModelContext;
import com.dynamicreflector.spoon.SpoonModelLoader;
import com.dynamicreflector.project.AndroidProjectLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PublicCliCommandTest {
    @TempDir
    private Path tempDir;

    @Test
    void noArgsShowsBannerAndHelp() {
        CapturedResult result = capture();

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Dynamic Reflector"));
        assertTrue(result.out.contains("  dynamic-reflector --analyse <projectPath>"));
        assertTrue(result.out.contains("  dynamic-reflector --examples"));
        assertTrue(result.out.contains("  dynamic-reflector --help"));
    }

    @Test
    void helpShowsBannerAndHelp() {
        CapturedResult result = capture("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Android Java Runtime Protection CLI"));
    }

    @Test
    void examplesShowExampleCommands() {
        CapturedResult result = capture("--examples");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Dynamic Reflector Examples"));
        assertTrue(result.out.contains("dynamic-reflector --inspect"));
        assertTrue(result.out.contains("dynamic-reflector --analyse"));
        assertTrue(result.out.contains("Short alias: dyrf"));
        assertTrue(!result.out.contains("dyrf --analyze"));
    }

    @Test
    void analyseAndAnalyzeBothRouteToInternalAnalyze() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        assertEquals(0, capture("--analyse", project.toString()).exitCode);
        assertEquals(0, capture("--analyze", project.toString()).exitCode);
    }

    @Test
    void inspectWorksWithUniqueShortClassName() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        CapturedResult result = capture("--inspect", project.toString(), "SamplePremiumHelper");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Class: SamplePremiumHelper"));
        assertTrue(result.out.contains("Resolved: com.example.hidelauncher.logic.SamplePremiumHelper"));
        assertTrue(result.out.contains("Supported methods:"));
        assertTrue(result.out.contains("* int calculate(int)"));
        assertTrue(result.out.contains("Skipped methods:"));
        assertTrue(result.out.contains("hidden(int) [private methods are skipped in Batch 2]"));
    }

    @Test
    void refactorDryRunWorksWithUniqueShortClassName() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        CapturedResult result = capture("--refactor", project.toString(), "SamplePremiumHelper", "--dry");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Dry run complete. No files were written."));
        assertTrue(result.out.contains("dynamic-reflector --refactor"));
        assertTrue(result.out.contains("--apply"));
    }

    @Test
    void oldInternalCommandsStillWork() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        assertEquals(0, capture("analyze", "--project", project.toString()).exitCode);
        assertEquals(0, capture("analyze-class", "--project", project.toString(), "--class", "SamplePremiumHelper").exitCode);
    }

    @Test
    void initAndVerifyPublicCommandsWork() throws Exception {
        Path project = createAndroidProject("android-app");

        CapturedResult init = capture("--init", project.toString());
        assertEquals(0, init.exitCode);
        assertTrue(init.out.contains("Created files: 5"));
        assertTrue(init.out.contains("Existing files: 0"));

        CapturedResult verify = capture("--verify", project.toString());
        assertEquals(0, verify.exitCode);
        assertTrue(verify.out.contains("Framework status: OK"));
        assertTrue(verify.out.contains("Required files: 5/5 found"));
        assertTrue(verify.out.contains("Build your Android project"));
    }

    @Test
    void publicCommandsHandleProjectPathsWithSpaces() throws Exception {
        Path project = createAndroidProject("Android Projects/TrashRush");
        writeSampleHelper(project);

        assertEquals(0, capture("--analyse", project.toString()).exitCode);
        assertEquals(0, capture("--inspect", project.toString(), "SamplePremiumHelper").exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "SamplePremiumHelper", "--dry").exitCode);
    }

    @Test
    void duplicateShortClassNameAsksForFullyQualifiedName() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);
        writeJava(
                project,
                "com/example/hidelauncher/other/SamplePremiumHelper.java",
                """
                        package com.example.hidelauncher.other;

                        public class SamplePremiumHelper {
                            public int calculate(int value) {
                                return value + 10;
                            }
                        }
                        """
        );

        CapturedResult result = capture("--inspect", project.toString(), "SamplePremiumHelper");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Duplicate class name"));
        assertTrue(result.err.contains("com.example.hidelauncher.logic.SamplePremiumHelper"));
        assertTrue(result.err.contains("com.example.hidelauncher.other.SamplePremiumHelper"));
    }

    @Test
    void missingClassShowsAvailableCandidates() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        CapturedResult result = capture("--inspect", project.toString(), "MissingClass");

        assertEquals(1, result.exitCode);
        assertTrue(result.err.contains("Class not found: MissingClass"));
        assertTrue(result.err.contains("Available candidate classes"));
        assertTrue(result.err.contains("com.example.hidelauncher.logic.SamplePremiumHelper"));
    }

    @Test
    void analyseOutputIncludesNextSuggestion() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);

        CapturedResult result = capture("--analyse", project.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Next:"));
        assertTrue(result.out.contains("dynamic-reflector --inspect"));
        assertTrue(result.out.contains("SamplePremiumHelper"));
    }

    @Test
    void inspectPrintsStaticMethodsNoneWhenNoneExist() throws Exception {
        Path project = createAndroidProject("android-app");
        writeJava(
                project,
                "com/example/hidelauncher/logic/NoStaticHelper.java",
                """
                        package com.example.hidelauncher.logic;

                        public class NoStaticHelper {
                            public int calculate(int value) {
                                return value * 2;
                            }
                        }
                        """
        );

        CapturedResult result = capture("--inspect", project.toString(), "NoStaticHelper");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Static methods:"));
        assertTrue(result.out.contains("  None"));
    }

    @Test
    void defaultMethodSignaturesUseShortTypeNames() throws Exception {
        Path project = createAndroidProject("android-app");
        writeJava(
                project,
                "com/example/hidelauncher/logic/Bin.java",
                """
                        package com.example.hidelauncher.logic;

                        public class Bin {
                        }
                        """
        );
        writeJava(
                project,
                "com/example/hidelauncher/logic/TrashItem.java",
                """
                        package com.example.hidelauncher.logic;

                        public class TrashItem {
                            public boolean collidesWith(Bin bin) {
                                return bin != null;
                            }
                        }
                        """
        );

        CapturedResult result = capture("--inspect", project.toString(), "TrashItem");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("boolean collidesWith(Bin)"));
        assertTrue(!result.out.contains("collidesWith(com.example.hidelauncher.logic.Bin)"));
    }

    @Test
    void applyOutputIncludesNextVerifyCommand() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);

        CapturedResult result = capture("--refactor", project.toString(), "SamplePremiumHelper", "--apply");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("created"));
        assertTrue(result.out.contains("Original class was not modified."));
        assertTrue(result.out.contains("dynamic-reflector --verify"));
    }

    @Test
    void verboseVerifyShowsFrameworkFilePaths() throws Exception {
        Path project = createAndroidProject("android-app");
        assertEquals(0, capture("--init", project.toString()).exitCode);

        CapturedResult result = capture("--verify", project.toString(), "--verbose");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Framework files:"));
        assertTrue(result.out.contains("AndroidDexPluginLoader.java"));
    }

    @Test
    void generatedProtectionWrapperIsNotCandidate() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "SamplePremiumHelper", "--apply").exitCode);

        JavaClassInfo wrapper = findClass(project, "com.example.hidelauncher.protection.wrapper.SamplePremiumHelperWrapper");

        assertEquals(ClassificationBucket.HARD_EXCLUDED, new FileClassifier().classify(wrapper).getBucket());
    }

    @Test
    void generatedProtectionApiIsNotCandidate() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "SamplePremiumHelper", "--apply").exitCode);

        JavaClassInfo api = findClass(project, "com.example.hidelauncher.protection.api.SamplePremiumHelperApi");

        assertEquals(ClassificationBucket.HARD_EXCLUDED, new FileClassifier().classify(api).getBucket());
    }

    @Test
    void existingRuntimeConfigManagerRegistryProtectionFilesRemainExcluded() throws Exception {
        Path project = createAndroidProject("android-app");
        assertEquals(0, capture("--init", project.toString()).exitCode);

        List<String> generatedClasses = List.of(
                "com.example.hidelauncher.protection.runtime.AndroidDexPluginLoader",
                "com.example.hidelauncher.protection.runtime.PluginException",
                "com.example.hidelauncher.protection.config.PluginConfig",
                "com.example.hidelauncher.protection.manager.PremiumFeatureManager",
                "com.example.hidelauncher.protection.registry.PluginRegistry"
        );
        FileClassifier classifier = new FileClassifier();
        for (String generatedClass : generatedClasses) {
            assertEquals(
                    ClassificationBucket.HARD_EXCLUDED,
                    classifier.classify(findClass(project, generatedClass)).getBucket(),
                    generatedClass
            );
        }
    }

    @Test
    void analyseOutputDoesNotListProtectionApiOrWrapperAsSafeCandidates() throws Exception {
        Path project = createAndroidProject("android-app");
        writeSampleHelper(project);
        assertEquals(0, capture("--init", project.toString()).exitCode);
        assertEquals(0, capture("--refactor", project.toString(), "SamplePremiumHelper", "--apply").exitCode);

        CapturedResult result = capture("--analyse", project.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Safe candidates (1):"));
        assertTrue(result.out.contains("* SamplePremiumHelper"));
        assertTrue(!result.out.contains("SamplePremiumHelperWrapper"));
        assertTrue(!result.out.contains("SamplePremiumHelperApi"));
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

    private Path createAndroidProject(String folderName) throws Exception {
        Path project = tempDir.resolve(folderName);
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

    private void writeSampleHelper(Path project) throws Exception {
        writeJava(
                project,
                "com/example/hidelauncher/logic/SamplePremiumHelper.java",
                """
                        package com.example.hidelauncher.logic;

                        public class SamplePremiumHelper {
                            public int calculate(int value) {
                                return value * 2;
                            }

                            private int hidden(int value) {
                                return value + 1;
                            }

                            public static int staticCalculate(int value) {
                                return value * 3;
                            }
                        }
                        """
        );
    }

    private void writeJava(Path project, String relativePath, String content) throws Exception {
        Path file = project.resolve("app/src/main/java").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private JavaClassInfo findClass(Path project, String qualifiedName) throws Exception {
        SpoonModelContext model = new SpoonModelLoader().load(new AndroidProjectLocator().locate(project, null));
        return new SpoonClassInspector()
                .listClasses(model)
                .stream()
                .filter(info -> info.getQualifiedName().equals(qualifiedName))
                .findFirst()
                .orElseThrow();
    }

    private record CapturedResult(int exitCode, String out, String err) {
    }
}
