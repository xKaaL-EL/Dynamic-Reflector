package com.dynamicreflector.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OptionalGradleBuildRunner {
    public int run(Path projectRoot) throws IOException, InterruptedException {
        List<String> command = resolveCommand(projectRoot);
        System.out.println();
        System.out.println("Running optional Gradle build: " + String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }

    private List<String> resolveCommand(Path projectRoot) {
        List<String> command = new ArrayList<>();
        Path windowsWrapper = projectRoot.resolve("gradlew.bat");
        Path unixWrapper = projectRoot.resolve("gradlew");
        if (Files.exists(windowsWrapper)) {
            command.add(windowsWrapper.toString());
        } else if (Files.exists(unixWrapper)) {
            command.add(unixWrapper.toString());
        } else {
            command.add("gradle");
        }
        command.add("build");
        return command;
    }
}
