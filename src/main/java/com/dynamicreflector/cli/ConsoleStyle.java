package com.dynamicreflector.cli;

import java.util.Locale;

public final class ConsoleStyle {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    private static boolean enabled;

    private ConsoleStyle() {
    }

    public static void configure(boolean noColor) {
        enabled = !noColor && supportsAnsi();
    }

    public static String green(String value) {
        return color(GREEN, value);
    }

    public static String cyan(String value) {
        return color(CYAN, value);
    }

    public static String yellow(String value) {
        return color(YELLOW, value);
    }

    public static String red(String value) {
        return color(RED, value);
    }

    public static String success(String value) {
        return green(value);
    }

    public static String warning(String value) {
        return yellow(value);
    }

    public static String error(String value) {
        return red(value);
    }

    public static String heading(String value) {
        return cyan(value);
    }

    private static String color(String code, String value) {
        return enabled ? code + value + RESET : value;
    }

    private static boolean supportsAnsi() {
        if (System.console() == null) {
            return false;
        }
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isBlank()) {
            return false;
        }
        String term = System.getenv("TERM");
        if (term != null && term.equalsIgnoreCase("dumb")) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return !os.contains("windows") || System.getenv("WT_SESSION") != null || System.getenv("ANSICON") != null;
    }
}
