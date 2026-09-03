package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTaskService;

/** Releases queued restore requests when the instance budget has capacity again. */
@Component
public class StorageRestoreQueueScheduler {
    private final StorageRestoreRequestRepository requests;
    private final StorageRestoreBudgetService budget;
    private final BackgroundTaskService tasks;

    public StorageRestoreQueueScheduler(StorageRestoreRequestRepository requests,
        StorageRestoreBudgetService budget, BackgroundTaskService tasks) {
        this.requests = requests;
        this.budget = budget;
        this.tasks = tasks;
    }

    @Scheduled(fixedDelayString = "${ikaros.storage.restore-queue-scan-ms:30000}")
    public void releaseQueuedRequests() {
        requests.findAllByStatusOrderByCreatedAtAsc(StorageRestoreRequestStatus.QUEUED)
            .concatMap(this::release)
            .onErrorResume(error -> Mono.empty())
            .subscribe();
    }

    private Mono<Void> release(StorageRestoreRequestEntity request) {
        return budget.evaluate(request.totalItems(), request.totalBytes(), null)
            .filter(decision -> decision != StorageRestoreBudgetDecision.QUEUED)
            .flatMap(decision -> tasks.submit("storage.restore", payload(request),
                "storage.restore:queued:" + request.id()))
            .flatMap(task -> requests.save(new StorageRestoreRequestEntity(request.id(), request.actorId(), request.scope(),
                request.scopeId(), StorageRestoreRequestStatus.REQUESTED, request.totalItems(), request.completedItems(),
                request.totalBytes(), request.errorSummary(), request.idempotencyKey(), task.id(), request.createdAt(),
                Instant.now(), request.budgetDecision(), request.selectedAttachmentIds(), request.version())))
            .then();
    }

    private Map<String, Object> payload(StorageRestoreRequestEntity request) {
        String scopeKey = request.scope() == StorageRestoreScope.SEASON ? "season_id" : "attachment_id";
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("restore_request_id", request.id().toString());
        payload.put(scopeKey, request.scopeId().toString());
        payload.put("provider_restore_class", "STANDARD");
        if (request.selectedAttachmentIds() != null && !request.selectedAttachmentIds().isBlank()) {
            payload.put("selected_attachment_ids", request.selectedAttachmentIds());
        }
        return payload;
    }
}
