package run.ikaros.integration.api;

import java.time.Instant;

public record DurableEventDeliveryStatus(long pendingCount, long attemptedPendingCount, Instant lastAttemptAt) { }
