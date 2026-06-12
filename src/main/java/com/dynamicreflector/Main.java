package com.dynamicreflector;

import com.dynamicreflector.cli.AnalyzeClassCommand;
import com.dynamicreflector.cli.AnalyzeCommand;
import com.dynamicreflector.cli.ApplyFrameworkCommand;
import com.dynamicreflector.cli.RefactorCommand;
import com.dynamicreflector.cli.VerifyCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "protection-tool",
        mixinStandardHelpOptions = true,
        version = "Dynamic Reflector 0.1.0",
        description = "Analyzes Android Java projects and generates a minimal DexClassLoader protection framework.",
        subcommands = {
                AnalyzeCommand.class,
                ApplyFrameworkCommand.class,
                AnalyzeClassCommand.class,
                RefactorCommand.class,
                VerifyCommand.class
        }
)
public final class Main implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}
