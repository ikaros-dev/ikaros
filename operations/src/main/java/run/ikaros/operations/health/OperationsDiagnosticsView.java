package run.ikaros.operations.health;

import java.time.Instant;
import run.ikaros.integration.api.DurableEventDeliveryStatus;

public record OperationsDiagnosticsView(
    long pendingTasks, long runningTasks, long failedTasks, long timedOutTasks,
    DurableEventDeliveryStatus eventDelivery
) { }
