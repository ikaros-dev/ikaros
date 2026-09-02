package run.ikaros.storage;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;

@Service
public class StorageProviderDrainService {
    private final StorageProviderRegistry providers;
    private final BackgroundTaskService tasks;

    public StorageProviderDrainService(StorageProviderRegistry providers, BackgroundTaskService tasks) {
        this.providers = providers; this.tasks = tasks;
    }

    public Mono<BackgroundTask> request(UUID providerId, UUID actorId) {
        return providers.drain(providerId)
            .then(tasks.submit("storage.provider-drain", Map.of("provider_id", providerId.toString(),
                "requested_by", actorId.toString()), "storage.provider-drain:" + providerId))
            .map(task -> task);
    }
}
