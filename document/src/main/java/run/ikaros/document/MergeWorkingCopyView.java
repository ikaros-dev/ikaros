package run.ikaros.document;

public record MergeWorkingCopyView(String content, boolean conflict, long currentVersion, String contentSchemaVersion) {}
