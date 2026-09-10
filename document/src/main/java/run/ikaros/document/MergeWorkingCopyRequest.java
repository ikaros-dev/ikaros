package run.ikaros.document;

import jakarta.validation.constraints.NotNull;

public record MergeWorkingCopyRequest(@NotNull String baseContent, @NotNull String localContent, String contentSchemaVersion) {}
