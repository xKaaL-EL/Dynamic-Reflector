package com.dynamicreflector.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class OriginalClassConverter {
    public OriginalClassConversionResult apply(OriginalClassConversionPlan plan)
            throws IOException, ConversionSafetyException {
        BackupStatus backupStatus = ensureBackup(plan);
        if (!plan.hasChanges()) {
            return new OriginalClassConversionResult(backupStatus, false);
        }
        Files.writeString(plan.getSourcePath(), plan.getConvertedContent(), StandardCharsets.UTF_8);
        return new OriginalClassConversionResult(backupStatus, true);
    }

    private BackupStatus ensureBackup(OriginalClassConversionPlan plan) throws IOException, ConversionSafetyException {
        if (!plan.hasChanges()) {
            return Files.exists(plan.getBackupPath()) ? BackupStatus.UNCHANGED : BackupStatus.NOT_NEEDED;
        }
        if (!Files.exists(plan.getBackupPath())) {
            Files.writeString(plan.getBackupPath(), plan.getOriginalContent(), StandardCharsets.UTF_8);
            return BackupStatus.CREATED;
        }
        if (!Files.isRegularFile(plan.getBackupPath())) {
            throw new ConversionSafetyException("Backup path exists but is not a regular file: " + plan.getBackupPath());
        }
        String backupContent = Files.readString(plan.getBackupPath(), StandardCharsets.UTF_8);
        if (!backupContent.contains("class " + plan.getRefactorPlan().getClassInfo().getSimpleName())) {
            throw new ConversionSafetyException("Existing backup does not look like the selected class backup: "
                    + plan.getBackupPath());
        }
        return BackupStatus.UNCHANGED;
    }
}
