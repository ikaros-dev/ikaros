package run.ikaros.storage;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.event.DurableEventService;

/** 收敛临时恢复副本对应的已过期 Restore Operation。 */
@Component
public class StorageRestoreOperationExpiryScheduler {
    private final StorageRestoreOperationRepository operations;
    private final DurableEventService events;
    private final AtomicBoolean running = new AtomicBoolean();

    public StorageRestoreOperationExpiryScheduler(StorageRestoreOperationRepository operations, DurableEventService events) {
        this.operations = operations;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${ikaros.storage.restore-operation-expiry-scan-ms:30000}")
    public void expire() {
        if (!running.compareAndSet(false, true)) return;
        Instant now = Instant.now();
        operations.findAllByStatusAndRestoreExpiresAtBefore(StorageRestoreOperationStatus.SUCCEEDED, now)
            .concatMap(operation -> operations.save(new StorageRestoreOperationEntity(operation.id(), operation.placementId(),
                operation.providerRestoreClass(), operation.restoreGeneration(), StorageRestoreOperationStatus.EXPIRED,
                operation.backgroundTaskId(), operation.providerOperationId(), operation.restoreExpiresAt(),
                operation.errorSummary(), operation.createdAt(), now, operation.version()))
                .flatMap(expired -> events.append("storage.restore-operation.expired", 1, "restore_operation", expired.id(),
                    "{\"operation_id\":\"" + expired.id() + "\",\"placement_id\":\"" + expired.placementId() + "\"}").then()))
            .onErrorResume(ignored -> Mono.empty())
            .doFinally(ignored -> running.set(false))
            .subscribe();
    }
}
