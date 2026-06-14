package com.dynamicreflector.cli;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.verify.FrameworkVerifier;
import com.dynamicreflector.verify.OptionalGradleBuildRunner;
import com.dynamicreflector.verify.VerifyResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "verify", description = "Verify generated framework files. Gradle build is optional.")
public final class VerifyCommand extends CommandSupport implements Callable<Integer> {
    @Option(names = "--project", required = true, description = "Android project path.")
    private Path projectPath;

    @Option(names = "--gradle-build", description = "Run Gradle build after lightweight verification.")
    private boolean gradleBuild;

    @Option(names = "--base-package", description = "Fallback base package if namespace/applicationId detection fails.")
    private String basePackage;

    @Override
    public Integer call() {
        try {
            AndroidProject project = locateProject(projectPath, basePackage);
            VerifyResult result = new FrameworkVerifier().verify(project);
            topSeparator();
            section("Framework Verification");
            printProject(project);
            System.out.println();
            printSummary(project, result);

            if (!result.isSuccess()) {
                return 1;
            }

            if (gradleBuild) {
                return new OptionalGradleBuildRunner().run(project.getProjectRoot());
            }
            return 0;
        } catch (Exception e) {
            return fail(e);
        }
    }

    private String formatVerifyMessage(AndroidProject project, String message) {
        int split = message.indexOf(' ');
        if (split < 0 || verbose) {
            return message;
        }
        String status = message.substring(0, split);
        Path path = Path.of(message.substring(split + 1));
        return status + " " + displayPath(project, path);
    }

    private void printSummary(AndroidProject project, VerifyResult result) {
        long found = result.getMessages().stream().filter(message -> message.startsWith("exists ")).count();
        int total = result.getMessages().size();
        System.out.println("Framework status: " + (result.isSuccess() ? ConsoleStyle.success("OK") : ConsoleStyle.error("FAILED")));
        System.out.println("Required files: " + found + "/" + total + " found");

        List<String> missing = result.getMessages()
                .stream()
                .filter(message -> message.startsWith("missing "))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            System.out.println();
            System.out.println(ConsoleStyle.error("Missing files:"));
            for (String message : missing) {
                Path path = Path.of(message.substring("missing ".length()));
                System.out.println("  * " + (verbose ? path : path.getFileName()));
            }
        }

        if (verbose) {
            System.out.println();
            System.out.println(ConsoleStyle.heading("Framework files:"));
            result.getMessages().forEach(message -> System.out.println("  * " + formatVerifyMessage(project, message)));
        }

        if (result.isSuccess()) {
            System.out.println();
            System.out.println(ConsoleStyle.heading("Next:"));
            System.out.println("  Build your Android project to confirm everything compiles.");
        }
    }
}
