package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.media.api.MediaRestoreTargetQuery;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.storage.api.BlobAvailability;
import run.ikaros.storage.api.PlacementState;
import run.ikaros.storage.api.StorageTier;

class StorageRestoreTaskHandlerTest {
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final StorageRestoreTaskHandler handler = new StorageRestoreTaskHandler(
        mock(BackgroundTaskDispatcher.class), mock(StorageRestoreRequestRepository.class),
        mock(AttachmentRepository.class), blobs, placements, mock(StorageProviderRegistry.class),
        mock(StorageRestoreExecutor.class), mock(StorageRestoreOperationRepository.class),
        mock(StorageRestoreRequestItemRepository.class), mock(MediaRestoreTargetQuery.class),
        mock(DurableEventPublisher.class));

    @Test
    void successfulRestoreActivatesPlacementAndBlob() {
        UUID blobId = UUID.randomUUID();
        BlobPlacementEntity placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "archive",
            StorageTier.ARCHIVE, "objects/archive.bin", PlacementState.RESTORING, Instant.now(), Instant.now(), 0L);
        BlobEntity blob = new BlobEntity(blobId, "hash", 128, "application/octet-stream",
            BlobAvailability.REMOTE, Instant.now(), 0L);
        when(placements.save(any(BlobPlacementEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(blobs.save(any(BlobEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(handler.activatePlacement(placement, blob, StorageRestoreOperationStatus.SUCCEEDED))
            .verifyComplete();

        org.mockito.ArgumentCaptor<BlobPlacementEntity> placementCaptor = org.mockito.ArgumentCaptor.forClass(BlobPlacementEntity.class);
        org.mockito.ArgumentCaptor<BlobEntity> blobCaptor = org.mockito.ArgumentCaptor.forClass(BlobEntity.class);
        org.mockito.Mockito.verify(placements).save(placementCaptor.capture());
        org.mockito.Mockito.verify(blobs).save(blobCaptor.capture());
        assertEquals(PlacementState.ACTIVE, placementCaptor.getValue().placementState());
        assertEquals(BlobAvailability.AVAILABLE, blobCaptor.getValue().availability());
        assertEquals(blobId, blobCaptor.getValue().id());
    }
}
