package run.ikaros.storage;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;

@Service
public class PersistentStoragePlacementTieringService implements StoragePlacementTieringService {
    private final BlobPlacementRepository placements;
    private final BackgroundTaskService tasks;
    private final DurableEventService events;

    public PersistentStoragePlacementTieringService(BlobPlacementRepository placements,
        BackgroundTaskService tasks, DurableEventService events) {
        this.placements = placements;
        this.tasks = tasks;
        this.events = events;
    }

    @Override
    public Mono<BackgroundTask> promote(UUID placementId, StorageTier targetTier, String idempotencyKey) {
        return submit(placementId, targetTier, "PROMOTE", idempotencyKey,
            "storage.placement.promotion-requested");
    }

    @Override
    public Mono<BackgroundTask> demote(UUID placementId, StorageTier targetTier, String idempotencyKey) {
        return submit(placementId, targetTier, "DEMOTE", idempotencyKey,
            "storage.placement.demotion-requested");
    }

    private Mono<BackgroundTask> submit(UUID placementId, StorageTier targetTier, String direction,
        String idempotencyKey, String eventType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        if (targetTier == null) return Mono.error(new IllegalArgumentException("目标存储层级不能为空"));
        return placements.findById(placementId)
            .switchIfEmpty(Mono.error(new NotFoundException("Placement 不存在")))
            .flatMap(placement -> {
                if (placement.storageTier() == targetTier) {
                    return Mono.error(new ConflictException("Placement 已经位于目标存储层级"));
                }
                int directionSign = targetTier.compareTo(placement.storageTier());
                if (("PROMOTE".equals(direction) && directionSign >= 0)
                    || ("DEMOTE".equals(direction) && directionSign <= 0)) {
                    return Mono.error(new ConflictException("Placement 层级方向不合法"));
                }
                Map<String, Object> payload = Map.of("placement_id", placementId.toString(),
                    "blob_id", placement.blobId().toString(), "direction", direction,
                    "target_tier", targetTier.name());
                return tasks.submit("storage.placement.tiering", payload, idempotencyKey)
                    .flatMap(task -> events.append(eventType, 1, "blob_placement", placementId,
                        "{\"placement_id\":\"" + placementId + "\",\"blob_id\":\"" + placement.blobId()
                            + "\",\"target_tier\":\"" + targetTier + "\"}").thenReturn(task));
            });
    }
}
