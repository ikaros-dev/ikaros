package run.ikaros.storage;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.event.DurableEventService;

/** Detects provider-side loss of Archive Base objects; it never treats it as a cache miss. */
@Component
public class ArchiveBaseReconciliationService {
    private final BlobPlacementRepository placements;
    private final BlobRepository blobs;
    private final StorageProviderRegistry providers;
    private final java.util.List<StorageProviderLifecycleInspector> inspectors;
    private final DurableEventService events;

    public ArchiveBaseReconciliationService(BlobPlacementRepository placements, BlobRepository blobs,
        StorageProviderRegistry providers, java.util.List<StorageProviderLifecycleInspector> inspectors,
        DurableEventService events) {
        this.placements = placements; this.blobs = blobs; this.providers = providers;
        this.inspectors = inspectors; this.events = events;
    }

    @Scheduled(fixedDelayString = "${ikaros.storage.archive-base-reconcile-ms:300000}")
    public void reconcile() {
        placements.findAllByDurabilityRole(PlacementDurabilityRole.ARCHIVE_BASE)
            .flatMap(this::inspect)
            .onErrorResume(ignored -> Mono.empty())
            .subscribe();
    }

    private Mono<Void> inspect(BlobPlacementEntity placement) {
        return Mono.zip(blobs.findById(placement.blobId()), providers.getByKey(placement.provider()))
            .flatMap(values -> inspectors.stream().filter(i -> i.supports(values.getT2())).findFirst()
                .map(i -> i.inspect(values.getT2(), placement, values.getT1()))
                .orElseGet(() -> Mono.just(StorageProviderLifecycleState.UNKNOWN)))
            .flatMap(state -> state == StorageProviderLifecycleState.MISSING
                ? events.append("storage.archive-base.missing", 1, "blob_placement", placement.id(),
                    "{\"placement_id\":\"" + placement.id() + "\",\"detected_at\":\"" + Instant.now() + "\"}").then()
                : Mono.empty());
    }
}
