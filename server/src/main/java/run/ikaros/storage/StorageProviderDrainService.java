package run.ikaros.storage;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;
import run.ikaros.event.DurableEventService;

@Service
public class StorageProviderDrainService {
    private final StorageProviderRegistry providers;
    private final BackgroundTaskService tasks;
    private final DurableEventService events;

    public StorageProviderDrainService(StorageProviderRegistry providers, BackgroundTaskService tasks, DurableEventService events) {
        this.providers = providers; this.tasks = tasks; this.events = events;
    }

    public Mono<BackgroundTask> request(UUID providerId, UUID actorId) {
        return request(providerId, actorId, "storage.provider-drain:" + providerId);
    }

    public Mono<BackgroundTask> request(UUID providerId, UUID actorId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        return providers.drain(providerId)
            .then(tasks.submit("storage.provider-drain", Map.of("provider_id", providerId.toString(),
                "requested_by", actorId.toString()), "storage.provider-drain:" + providerId + ":" + idempotencyKey))
            .flatMap(task -> events.append("storage.provider.drain-requested", 1, "storage_provider", providerId,
                "{\"provider_id\":\"" + providerId + "\",\"task_id\":\"" + task.id() + "\"}").thenReturn(task));
    }
}
