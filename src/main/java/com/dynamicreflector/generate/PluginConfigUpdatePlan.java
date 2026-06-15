package com.dynamicreflector.generate;

public final class PluginConfigUpdatePlan {
    private final String content;
    private final GeneratedFileStatus mappingStatus;
    private final GeneratedFileStatus allowedPrefixStatus;

    public PluginConfigUpdatePlan(
            String content,
            GeneratedFileStatus mappingStatus,
            GeneratedFileStatus allowedPrefixStatus
    ) {
        this.content = content;
        this.mappingStatus = mappingStatus;
        this.allowedPrefixStatus = allowedPrefixStatus;
    }

    public String getContent() {
        return content;
    }

    public GeneratedFileStatus getMappingStatus() {
        return mappingStatus;
    }

    public GeneratedFileStatus getAllowedPrefixStatus() {
        return allowedPrefixStatus;
    }
}
