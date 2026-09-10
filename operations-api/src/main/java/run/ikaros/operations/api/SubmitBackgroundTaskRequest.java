package run.ikaros.operations.api;

import java.util.Map;

/** HTTP/Application request for durably enqueueing one background task. */
public record SubmitBackgroundTaskRequest(String type, Map<String, Object> payload) {
}
