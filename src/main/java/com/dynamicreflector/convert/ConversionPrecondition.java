package com.dynamicreflector.convert;

public final class ConversionPrecondition {
    private final String label;
    private final boolean success;
    private final String command;

    public ConversionPrecondition(String label, boolean success, String command) {
        this.label = label;
        this.success = success;
        this.command = command;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCommand() {
        return command;
    }
}
