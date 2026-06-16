package com.dynamicreflector.convert;

import com.dynamicreflector.refactor.MethodPlan;
import com.dynamicreflector.refactor.RefactorPlan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class OriginalClassConversionPlan {
    private final RefactorPlan refactorPlan;
    private final Path sourcePath;
    private final Path backupPath;
    private final String originalContent;
    private final String convertedContent;
    private final List<MethodConversionPlan> methodPlans;
    private final List<MethodPlan> skippedMethods;
    private final ImportUpdateStatus apiImportStatus;
    private final ImportUpdateStatus managerImportStatus;
    private final String preview;

    public OriginalClassConversionPlan(
            RefactorPlan refactorPlan,
            Path sourcePath,
            Path backupPath,
            String originalContent,
            String convertedContent,
            List<MethodConversionPlan> methodPlans,
            List<MethodPlan> skippedMethods,
            ImportUpdateStatus apiImportStatus,
            ImportUpdateStatus managerImportStatus,
            String preview
    ) {
        this.refactorPlan = refactorPlan;
        this.sourcePath = sourcePath;
        this.backupPath = backupPath;
        this.originalContent = originalContent;
        this.convertedContent = convertedContent;
        this.methodPlans = Collections.unmodifiableList(methodPlans);
        this.skippedMethods = Collections.unmodifiableList(skippedMethods);
        this.apiImportStatus = apiImportStatus;
        this.managerImportStatus = managerImportStatus;
        this.preview = preview;
    }

    public RefactorPlan getRefactorPlan() {
        return refactorPlan;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Path getBackupPath() {
        return backupPath;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getConvertedContent() {
        return convertedContent;
    }

    public List<MethodConversionPlan> getMethodPlans() {
        return methodPlans;
    }

    public List<MethodPlan> getSkippedMethods() {
        return skippedMethods;
    }

    public ImportUpdateStatus getApiImportStatus() {
        return apiImportStatus;
    }

    public ImportUpdateStatus getManagerImportStatus() {
        return managerImportStatus;
    }

    public String getPreview() {
        return preview;
    }

    public boolean hasChanges() {
        return !originalContent.equals(convertedContent);
    }

    public long convertedMethodCount() {
        return methodPlans.stream().filter(plan -> plan.getStatus() == MethodConversionStatus.CONVERT).count();
    }

    public long unchangedMethodCount() {
        return methodPlans.stream().filter(plan -> plan.getStatus() == MethodConversionStatus.UNCHANGED).count();
    }
}
