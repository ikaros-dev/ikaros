package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.util.UUID;
import run.ikaros.task.BackgroundTask;
import reactor.core.publisher.Mono;

public interface StoragePlacementTieringService {
    Mono<BackgroundTask> promote(UUID placementId, StorageTier targetTier, String idempotencyKey);
    Mono<BackgroundTask> demote(UUID placementId, StorageTier targetTier, String idempotencyKey);
}
