package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.ConflictException;
import run.ikaros.storage.api.BlobAvailability;
import run.ikaros.storage.api.PlacementState;
import run.ikaros.storage.api.StorageTier;

class BlobGarbageCollectorTest {
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final AttachmentRepository attachments = mock(AttachmentRepository.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final TransactionalOperator transaction = mock(TransactionalOperator.class);
    private final BlobRetentionHoldRepository holds = mock(BlobRetentionHoldRepository.class);
    private final run.ikaros.storage.api.DeliveryLeaseService leases = mock(run.ikaros.storage.api.DeliveryLeaseService.class);
    private final StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
    private final StorageContentDeleter deleter = mock(StorageContentDeleter.class);
    private final BlobGarbageCollector collector = new BlobGarbageCollector(blobs, attachments, placements, transaction,
        holds, leases, providers, deleter);

    @Test
    void activeAttachmentReferenceBlocksPurge() {
        UUID blobId = UUID.randomUUID();
        stubBlob(blobId);
        when(attachments.countByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(blobId)).thenReturn(Mono.just(1L));

        StepVerifier.create(collector.purge(blobId))
            .expectErrorSatisfies(error -> assertInstanceOf(ConflictException.class, error))
            .verify();

        verify(placements, never()).deleteByBlobId(blobId);
        verify(blobs, never()).deleteById(blobId);
    }

    @Test
    void activeRetentionHoldBlocksPurgeAfterReferenceCheck() {
        UUID blobId = UUID.randomUUID();
        stubBlob(blobId);
        when(attachments.countByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(blobId)).thenReturn(Mono.just(0L));
        when(holds.existsActiveByBlobId(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(collector.purge(blobId))
            .expectErrorSatisfies(error -> assertInstanceOf(ConflictException.class, error))
            .verify();

        verify(placements, never()).deleteByBlobId(blobId);
        verify(blobs, never()).deleteById(blobId);
    }

    @Test
    void archiveBaseBlocksPurgeAfterAllGuardsPass() {
        UUID blobId = UUID.randomUUID();
        stubBlob(blobId);
        when(attachments.countByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(blobId)).thenReturn(Mono.just(0L));
        when(holds.existsActiveByBlobId(any(), any())).thenReturn(Mono.just(false));
        when(leases.protectsBlob(blobId)).thenReturn(Mono.just(false));
        BlobPlacementEntity archiveBase = new BlobPlacementEntity(UUID.randomUUID(), blobId, "archive", StorageTier.ARCHIVE,
            "objects/blob", PlacementState.UNAVAILABLE, PlacementDurabilityRole.ARCHIVE_BASE, false, true, null, null,
            null, null, Instant.now(), Instant.now(), 0L);
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(archiveBase));

        StepVerifier.create(collector.purge(blobId))
            .expectErrorSatisfies(error -> assertInstanceOf(ConflictException.class, error))
            .verify();

        verify(placements, never()).deleteByBlobId(blobId);
        verify(blobs, never()).deleteById(blobId);
    }

    private void stubBlob(UUID blobId) {
        when(blobs.findById(blobId)).thenReturn(Mono.just(new BlobEntity(blobId, "a", 1, "application/octet-stream",
            BlobAvailability.AVAILABLE, Instant.now(), 0L)));
        when(holds.existsActiveByBlobId(any(), any())).thenReturn(Mono.just(false));
        when(leases.protectsBlob(blobId)).thenReturn(Mono.just(false));
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.empty());
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
