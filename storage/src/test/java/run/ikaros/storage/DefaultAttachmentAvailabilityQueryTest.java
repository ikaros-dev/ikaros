package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import run.ikaros.storage.api.BlobAvailability;
import run.ikaros.storage.api.PlacementState;
import run.ikaros.storage.api.StorageTier;

class DefaultAttachmentAvailabilityQueryTest {
    private final AttachmentReferenceQuery references = mock(AttachmentReferenceQuery.class);
    private final AttachmentRepository attachments = mock(AttachmentRepository.class);
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
    private final StorageRestoreOperationRepository operations = mock(StorageRestoreOperationRepository.class);
    private final DefaultAttachmentAvailabilityQuery query = new DefaultAttachmentAvailabilityQuery(references,
        attachments, blobs, placements, providers, operations);

    @Test
    void reportsCorruptedBlobWithoutTreatingItAsReadable() {
        Fixture fixture = fixture(BlobAvailability.CORRUPTED);

        StepVerifier.create(query.get(fixture.actorId, fixture.attachmentId))
            .assertNext(result -> assertEquals(AttachmentAvailabilityStatus.CORRUPTED, result.status()))
            .verifyComplete();
    }

    @Test
    void reportsMissingWhenNoPlacementCanBeRead() {
        Fixture fixture = fixture(BlobAvailability.REMOTE);
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(fixture.blobId)).thenReturn(Flux.empty());

        StepVerifier.create(query.get(fixture.actorId, fixture.attachmentId))
            .assertNext(result -> assertEquals(AttachmentAvailabilityStatus.MISSING, result.status()))
            .verifyComplete();
    }

    private Fixture fixture(BlobAvailability availability) {
        UUID actorId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        when(references.requireReadable(actorId, attachmentId)).thenReturn(Mono.just(new AttachmentReference(attachmentId, resourceId)));
        when(attachments.findById(attachmentId)).thenReturn(Mono.just(new AttachmentEntity(attachmentId, resourceId, blobId,
            "data.bin", run.ikaros.storage.api.AttachmentKind.ORIGINAL, now, null, 0L)));
        when(blobs.findById(blobId)).thenReturn(Mono.just(new BlobEntity(blobId, "a", 1, "application/octet-stream",
            availability, now, 0L)));
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(new BlobPlacementEntity(
            UUID.randomUUID(), blobId, "archive", StorageTier.ARCHIVE, "data.bin", PlacementState.UNAVAILABLE,
            now, now, 0L)));
        return new Fixture(actorId, attachmentId, blobId);
    }

    private record Fixture(UUID actorId, UUID attachmentId, UUID blobId) {}
}
