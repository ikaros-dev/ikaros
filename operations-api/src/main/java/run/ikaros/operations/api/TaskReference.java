package run.ikaros.operations.api;

import java.util.UUID;

/** Stable reference to a persisted background task. */
public record TaskReference(UUID taskId, String taskType) { }
