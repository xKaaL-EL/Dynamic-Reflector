package com.dynamicreflector.cli;

import com.dynamicreflector.Main;
import com.dynamicreflector.generate.FrameworkGenerator;
import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.project.AndroidProjectLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RefactorCommandTest {
    @TempDir
    private Path tempDir;

    @Test
    void dryRunMakesNoWrites() throws Exception {
        Path project = createAndroidProject();
        writeSampleHelper(project);

        int exitCode = execute(
                "refactor",
                "--project", project.toString(),
                "--class", "com.example.hidelauncher.logic.SamplePremiumHelper",
                "--dry-run"
        );

        assertEquals(0, exitCode);
        assertFalse(Files.exists(apiPath(project, "SamplePremiumHelperApi")));
        assertFalse(Files.exists(wrapperPath(project, "SamplePremiumHelperWrapper")));
    }

    @Test
    void unsafeAndroidComponentClassIsRejected() throws Exception {
        Path project = createAndroidProject();
        writeJava(
                project,
                "com/example/hidelauncher/MyApplication.java",
                """
                        package com.example.hidelauncher;

                        import android.app.Application;

                        public class MyApplication extends Application {
                            public void premiumLogic() {
                            }
                        }
                        """
        );

        int exitCode = execute(
                "refactor",
                "--project", project.toString(),
                "--class", "com.example.hidelauncher.MyApplication",
                "--dry-run"
        );

        assertEquals(1, exitCode);
        assertFalse(Files.exists(apiPath(project, "MyApplicationApi")));
        assertFalse(Files.exists(wrapperPath(project, "MyApplicationWrapper")));
    }

    @Test
    void safePlainJavaClassGeneratesApiAndWrapper() throws Exception {
        Path project = createAndroidProject();
        writeSampleHelper(project);
        applyFramework(project);

        int exitCode = execute(
                "refactor",
                "--project", project.toString(),
                "--class", "com.example.hidelauncher.logic.SamplePremiumHelper",
                "--apply"
        );

        assertEquals(0, exitCode);

        Path api = apiPath(project, "SamplePremiumHelperApi");
        Path wrapper = wrapperPath(project, "SamplePremiumHelperWrapper");
        assertTrue(Files.exists(api));
        assertTrue(Files.exists(wrapper));

        String apiContent = Files.readString(api, StandardCharsets.UTF_8);
        String wrapperContent = Files.readString(wrapper, StandardCharsets.UTF_8);
        assertTrue(apiContent.contains("public interface SamplePremiumHelperApi"));
        assertTrue(apiContent.contains("int calculate(int value);"));
        assertFalse(apiContent.contains("hidden"));
        assertFalse(apiContent.contains("staticCalculate"));
        assertTrue(wrapperContent.contains("public final class SamplePremiumHelperWrapper implements SamplePremiumHelperApi"));
        assertTrue(wrapperContent.contains("UnsupportedOperationException"));
    }

    @Test
    void staticOnlyHelperIsRejectedWithNoWrites() throws Exception {
        Path project = createAndroidProject();
        writeJava(
                project,
                "com/example/hidelauncher/logic/StaticOnlyHelper.java",
                """
                        package com.example.hidelauncher.logic;

                        public class StaticOnlyHelper {
                            public static int staticCalculate(int value) {
                                return value * 3;
                            }
                        }
                        """
        );

        int exitCode = execute(
                "refactor",
                "--project", project.toString(),
                "--class", "com.example.hidelauncher.logic.StaticOnlyHelper",
                "--dry-run"
        );

        assertEquals(1, exitCode);
        assertFalse(Files.exists(apiPath(project, "StaticOnlyHelperApi")));
        assertFalse(Files.exists(wrapperPath(project, "StaticOnlyHelperWrapper")));
    }

    @Test
    void applyTwiceIsIdempotent() throws Exception {
        Path project = createAndroidProject();
        writeSampleHelper(project);
        applyFramework(project);

        String[] args = {
                "refactor",
                "--project", project.toString(),
                "--class", "com.example.hidelauncher.logic.SamplePremiumHelper",
                "--apply"
        };

        assertEquals(0, execute(args));
        Path api = apiPath(project, "SamplePremiumHelperApi");
        Path wrapper = wrapperPath(project, "SamplePremiumHelperWrapper");
        String firstApi = Files.readString(api, StandardCharsets.UTF_8);
        String firstWrapper = Files.readString(wrapper, StandardCharsets.UTF_8);

        assertEquals(0, execute(args));
        assertEquals(firstApi, Files.readString(api, StandardCharsets.UTF_8));
        assertEquals(firstWrapper, Files.readString(wrapper, StandardCharsets.UTF_8));
    }

    private int execute(String... args) {
        return new CommandLine(new Main()).execute(args);
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

    private void applyFramework(Path projectRoot) throws Exception {
        AndroidProject project = new AndroidProjectLocator().locate(projectRoot, null);
        new FrameworkGenerator().generateMinimalFramework(project);
    }

    private Path apiPath(Path project, String fileName) {
        return project.resolve("app/src/main/java/com/example/hidelauncher/protection/api").resolve(fileName + ".java");
    }

    private Path wrapperPath(Path project, String fileName) {
        return project.resolve("app/src/main/java/com/example/hidelauncher/protection/wrapper").resolve(fileName + ".java");
    }
}
