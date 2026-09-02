package run.ikaros.drive;
public record SyncMutationResult(String operationId, boolean applied, DriveNodeView node, String errorCode,
    String errorMessage) {}
