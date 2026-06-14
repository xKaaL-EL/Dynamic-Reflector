package com.dynamicreflector;

import com.dynamicreflector.cli.AnalyzeClassCommand;
import com.dynamicreflector.cli.AnalyzeCommand;
import com.dynamicreflector.cli.ApplyFrameworkCommand;
import com.dynamicreflector.cli.ConsoleStyle;
import com.dynamicreflector.cli.RefactorCommand;
import com.dynamicreflector.cli.VerifyCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;

@Command(
        name = "dynamic-reflector",
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
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    public static int execute(String... args) {
        try {
            boolean noColor = contains(args, "--no-color");
            ConsoleStyle.configure(noColor);
            String[] normalizedArgs = removeNoColor(args);
            String[] routedArgs = routePublicCommand(normalizedArgs);
            if (routedArgs == null) {
                return 0;
            }
            return new CommandLine(new Main()).execute(routedArgs);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            printHelp();
            return 1;
        }
    }

    @Override
    public void run() {
        printHelp();
    }

    private static String[] routePublicCommand(String[] args) {
        if (args.length == 0 || isHelp(args)) {
            printHelp();
            return null;
        }
        if (args.length == 1 && "--examples".equals(args[0])) {
            printExamples();
            return null;
        }

        String command = args[0];
        return switch (command) {
            case "--analyse", "--analyze" -> analyzeArgs(args);
            case "--inspect" -> inspectArgs(args);
            case "--init" -> initArgs(args);
            case "--refactor" -> refactorArgs(args);
            case "--verify" -> verifyArgs(args);
            default -> args;
        };
    }

    private static boolean isHelp(String[] args) {
        return args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]));
    }

    private static boolean contains(String[] args, String value) {
        for (String arg : args) {
            if (value.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String[] removeNoColor(String[] args) {
        List<String> filtered = new ArrayList<>();
        for (String arg : args) {
            if (!"--no-color".equals(arg)) {
                filtered.add(arg);
            }
        }
        return filtered.toArray(String[]::new);
    }

    private static String[] analyzeArgs(String[] args) {
        requireArgCount(args, 2, "--analyse <projectPath>");
        return appendVerboseIfPresent(args, "analyze", "--project", args[1]);
    }

    private static String[] inspectArgs(String[] args) {
        requireArgCount(args, 3, "--inspect <projectPath> <ClassName>");
        return appendVerboseIfPresent(args, "analyze-class", "--project", args[1], "--class", args[2]);
    }

    private static String[] initArgs(String[] args) {
        requireArgCount(args, 2, "--init <projectPath>");
        return appendVerboseIfPresent(args, "apply-framework", "--project", args[1], "--minimal");
    }

    private static String[] refactorArgs(String[] args) {
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException("Expected: --refactor <projectPath> <ClassName> --dry|--apply");
        }
        String mode = switch (args[3]) {
            case "--dry" -> "--dry-run";
            case "--apply" -> "--apply";
            default -> throw new IllegalArgumentException("Expected --dry or --apply for --refactor.");
        };
        return appendVerboseIfPresent(args, "refactor", "--project", args[1], "--class", args[2], "--strategy", "wrapper", mode);
    }

    private static String[] verifyArgs(String[] args) {
        requireArgCount(args, 2, "--verify <projectPath>");
        return appendVerboseIfPresent(args, "verify", "--project", args[1]);
    }

    private static void requireArgCount(String[] args, int expectedWithoutVerbose, String usage) {
        if (args.length != expectedWithoutVerbose && args.length != expectedWithoutVerbose + 1) {
            throw new IllegalArgumentException("Expected: " + usage);
        }
        if (args.length == expectedWithoutVerbose + 1 && !"--verbose".equals(args[expectedWithoutVerbose])) {
            throw new IllegalArgumentException("Unexpected argument: " + args[expectedWithoutVerbose]);
        }
    }

    private static String[] appendVerboseIfPresent(String[] originalArgs, String... mappedArgs) {
        boolean verbose = originalArgs.length > 0 && "--verbose".equals(originalArgs[originalArgs.length - 1]);
        if (!verbose) {
            return mappedArgs;
        }
        String[] withVerbose = new String[mappedArgs.length + 1];
        System.arraycopy(mappedArgs, 0, withVerbose, 0, mappedArgs.length);
        withVerbose[mappedArgs.length] = "--verbose";
        return withVerbose;
    }

    private static void printHelp() {
        System.out.println();
        System.out.println(ConsoleStyle.green("===================================================="));
        System.out.println(ConsoleStyle.green("Dynamic Reflector"));
        System.out.println(ConsoleStyle.green("Android Java Runtime Protection CLI"));
        System.out.println(ConsoleStyle.green("==================================="));
        System.out.println();
        System.out.println("Analyze Android Java projects and prepare selected");
        System.out.println("business-logic classes for safe runtime protection.");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Usage:"));
        System.out.println("  dynamic-reflector --analyse <projectPath>");
        System.out.println("  dynamic-reflector --inspect <projectPath> <ClassName>");
        System.out.println("  dynamic-reflector --init <projectPath>");
        System.out.println("  dynamic-reflector --refactor <projectPath> <ClassName> --dry");
        System.out.println("  dynamic-reflector --refactor <projectPath> <ClassName> --apply");
        System.out.println("  dynamic-reflector --verify <projectPath>");
        System.out.println("  dynamic-reflector --examples");
        System.out.println("  dynamic-reflector --help");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Alias:"));
        System.out.println("  dyrf");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Try:"));
        System.out.println("  dynamic-reflector --examples");
        System.out.println();
        System.out.println("PowerShell from this folder: .\\dynamic-reflector.bat --help");
        System.out.println("After adding this folder to PATH: dynamic-reflector --help");
    }

    private static void printExamples() {
        System.out.println();
        System.out.println(ConsoleStyle.green("Dynamic Reflector Examples"));
        System.out.println("----------------------------------------");
        System.out.println("Short alias: dyrf");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Analyze a project:"));
        System.out.println("  dynamic-reflector --analyse \"D:\\Android Projects\\TrashRush\"");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Inspect one class:"));
        System.out.println("  dynamic-reflector --inspect \"D:\\Android Projects\\TrashRush\" TrashItem");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Generate the minimal runtime framework:"));
        System.out.println("  dynamic-reflector --init \"D:\\Android Projects\\TrashRush\"");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Dry-run API/wrapper preparation:"));
        System.out.println("  dynamic-reflector --refactor \"D:\\Android Projects\\TrashRush\" TrashItem --dry");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Apply API/wrapper preparation:"));
        System.out.println("  dynamic-reflector --refactor \"D:\\Android Projects\\TrashRush\" TrashItem --apply");
        System.out.println();
        System.out.println(ConsoleStyle.heading("Verify generated framework files:"));
        System.out.println("  dynamic-reflector --verify \"D:\\Android Projects\\TrashRush\"");
        System.out.println();
        System.out.println("Add --verbose to show full paths and detailed project metadata.");
        System.out.println("From PowerShell in this folder, use .\\dynamic-reflector.bat --help.");
    }
}
