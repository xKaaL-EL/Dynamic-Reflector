package com.dynamicreflector.convert;

public final class OriginalClassConversionResult {
    private final BackupStatus backupStatus;
    private final boolean sourceChanged;

    public OriginalClassConversionResult(BackupStatus backupStatus, boolean sourceChanged) {
        this.backupStatus = backupStatus;
        this.sourceChanged = sourceChanged;
    }

    public BackupStatus getBackupStatus() {
        return backupStatus;
    }

    public boolean isSourceChanged() {
        return sourceChanged;
    }
}
