package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.storage.api.PlacementState;
import run.ikaros.storage.api.StorageTier;

class PersistentStoragePlacementTieringServiceTest {
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final BackgroundTaskService tasks = mock(BackgroundTaskService.class);
    private final DurableEventPublisher events = mock(DurableEventPublisher.class);
    private final PersistentStoragePlacementTieringService service = new PersistentStoragePlacementTieringService(
        placements, tasks, events);

    @Test
    void submitsPromotionFromExistingPlacement() {
        UUID placementId = UUID.randomUUID();
        BlobPlacementEntity placement = new BlobPlacementEntity(placementId, UUID.randomUUID(), "cold", StorageTier.COLD,
            "objects/blob", PlacementState.ACTIVE, Instant.now(), Instant.now(), 0L);
        BackgroundTask task = mock(BackgroundTask.class);
        when(placements.findById(placementId)).thenReturn(Mono.just(placement));
        when(tasks.findByTaskTypeAndIdempotencyKey("storage.placement.tiering", "repair-key")).thenReturn(Mono.empty());
        when(tasks.submit(any(), any(), any())).thenReturn(Mono.just(task));
        when(events.append(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.promote(placementId, StorageTier.HOT, "repair-key"))
            .assertNext(result -> assertSame(task, result))
            .verifyComplete();

        verify(tasks).submit("storage.placement.tiering", java.util.Map.of("placement_id", placementId.toString(),
            "blob_id", placement.blobId().toString(), "direction", "PROMOTE", "target_tier", "HOT"), "repair-key");
        verify(events).append(any());
    }

    @Test
    void reusesExistingPromotionTaskIdempotently() {
        UUID placementId = UUID.randomUUID();
        BlobPlacementEntity placement = new BlobPlacementEntity(placementId, UUID.randomUUID(), "cold", StorageTier.COLD,
            "objects/blob", PlacementState.ACTIVE, Instant.now(), Instant.now(), 0L);
        BackgroundTask task = mock(BackgroundTask.class);
        when(placements.findById(placementId)).thenReturn(Mono.just(placement));
        when(tasks.findByTaskTypeAndIdempotencyKey("storage.placement.tiering", "repair-key")).thenReturn(Mono.just(task));

        StepVerifier.create(service.promote(placementId, StorageTier.HOT, "repair-key"))
            .assertNext(result -> assertSame(task, result))
            .verifyComplete();

        verify(tasks, never()).submit(any(), any(), any());
        verifyNoInteractions(events);
    }

    @Test
    void rejectsMissingPlacementBeforeSubmittingRepair() {
        UUID placementId = UUID.randomUUID();
        when(placements.findById(placementId)).thenReturn(Mono.empty());

        StepVerifier.create(service.promote(placementId, StorageTier.COLD, "repair-key"))
            .expectError(NotFoundException.class)
            .verify();

        verifyNoInteractions(tasks, events);
    }
}
