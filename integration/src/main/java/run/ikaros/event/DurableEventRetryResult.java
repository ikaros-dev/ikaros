package run.ikaros.event;

import java.util.UUID;

public record DurableEventRetryResult(UUID eventId, long dispatchedCount) { }
