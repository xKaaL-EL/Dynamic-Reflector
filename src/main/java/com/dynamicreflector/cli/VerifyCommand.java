package com.dynamicreflector.cli;

import com.dynamicreflector.project.AndroidProject;
import com.dynamicreflector.verify.FrameworkVerifier;
import com.dynamicreflector.verify.OptionalGradleBuildRunner;
import com.dynamicreflector.verify.VerifyResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

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
            printProject(project);
            System.out.println();
            System.out.println("Framework verification:");
            result.getMessages().forEach(message -> System.out.println("  - " + message));

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
}
