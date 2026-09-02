package run.ikaros.storage;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskDispatcher;

@Component
public class StorageProviderDrainTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final StorageProviderRegistry providers;
    private final BlobPlacementRepository placements;

    public StorageProviderDrainTaskHandler(BackgroundTaskDispatcher dispatcher, StorageProviderRegistry providers,
                                           BlobPlacementRepository placements) {
        this.dispatcher = dispatcher; this.providers = providers; this.placements = placements;
    }

    @PostConstruct
    void register() { dispatcher.register("storage.provider-drain", this::handle); }

    private Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID providerId = UUID.fromString(String.valueOf(task.payload().get("provider_id")));
        return providers.get(providerId).flatMap(provider -> placements.countByProviderAndPlacementState(provider.providerKey(), PlacementState.ACTIVE)
            .flatMap(active -> {
                if (active > 0) return Mono.error(new ConflictException("Provider 仍存在 Active Placement，Drain 暂停"));
                return providers.disable(providerId).map(disabled -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("provider_id", providerId.toString());
                    result.put("status", disabled.status().name());
                    result.put("active_placements", 0);
                    return result;
                });
            }));
    }
}
