package run.ikaros.storage;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskDispatcher;

@Component
public class StoragePlacementTieringTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final BlobPlacementRepository placements;
    private final BlobRepository blobs;
    private final StorageProviderRegistry providers;
    private final StorageRestoreExecutor restoreExecutor;
    private final MediaDeliveryLeaseRepository leases;
    private final DurableEventService events;

    public StoragePlacementTieringTaskHandler(BackgroundTaskDispatcher dispatcher, BlobPlacementRepository placements,
        BlobRepository blobs, StorageProviderRegistry providers, StorageRestoreExecutor restoreExecutor,
        MediaDeliveryLeaseRepository leases, DurableEventService events) {
        this.dispatcher = dispatcher;
        this.placements = placements;
        this.blobs = blobs;
        this.providers = providers;
        this.restoreExecutor = restoreExecutor;
        this.leases = leases;
        this.events = events;
    }

    @PostConstruct
    void register() { dispatcher.register("storage.placement.tiering", this::handle); }

    private Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID placementId = UUID.fromString(task.payload().get("placement_id").toString());
        StorageTier targetTier = StorageTier.valueOf(task.payload().get("target_tier").toString());
        String direction = task.payload().get("direction").toString();
        return placements.findById(placementId)
            .switchIfEmpty(Mono.error(new NotFoundException("Placement 不存在")))
            .flatMap(placement -> "DEMOTE".equals(direction)
                ? demote(placement, targetTier)
                : promote(placement, targetTier))
            .flatMap(saved -> events.append("storage.placement.tiering-completed", 1, "blob_placement", saved.id(),
                "{\"placement_id\":\"" + saved.id() + "\",\"target_tier\":\"" + saved.storageTier() + "\"}")
                .thenReturn(Map.of("placement_id", saved.id().toString(), "target_tier", saved.storageTier().name())));
    }

    private Mono<BlobPlacementEntity> demote(BlobPlacementEntity placement, StorageTier targetTier) {
        return Mono.zip(
            leases.existsByBlobIdAndReleasedAtIsNullAndLeaseExpiresAtAfter(placement.blobId(), Instant.now()),
            placements.countByBlobIdAndPlacementState(placement.blobId(), PlacementState.ACTIVE))
            .flatMap(values -> values.getT1() && values.getT2() <= 1
                ? Mono.error(new IllegalStateException("Active Lease 阻止唯一可读 Placement Demotion"))
                : saveTier(placement, targetTier));
    }

    private Mono<BlobPlacementEntity> promote(BlobPlacementEntity placement, StorageTier targetTier) {
        if (placement.placementState() == PlacementState.ACTIVE) return saveTier(placement, targetTier);
        return Mono.zip(blobs.findById(placement.blobId())
                .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在"))),
            providers.getByKey(placement.provider())
                .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在"))))
            .flatMap(values -> {
                StorageProvider provider = values.getT2();
                if (!restoreExecutor.supports(provider)) return Mono.error(new ConflictException("Provider 不支持 Promotion"));
                return restoreExecutor.restore(provider, placement, values.getT1())
                    .flatMap(result -> result.readable()
                        ? saveActiveTier(placement, targetTier)
                        : Mono.error(new ConflictException("Promotion 后对象仍不可读")));
            });
    }

    private Mono<BlobPlacementEntity> saveTier(BlobPlacementEntity placement, StorageTier targetTier) {
        return placements.save(new BlobPlacementEntity(placement.id(), placement.blobId(), placement.provider(), targetTier,
            placement.objectKey(), placement.placementState(), placement.verifiedAt(), placement.createdAt(), placement.version()));
    }

    private Mono<BlobPlacementEntity> saveActiveTier(BlobPlacementEntity placement, StorageTier targetTier) {
        return placements.save(new BlobPlacementEntity(placement.id(), placement.blobId(), placement.provider(), targetTier,
            placement.objectKey(), PlacementState.ACTIVE, Instant.now(), placement.createdAt(), placement.version()));
    }
}
