package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.resource.api.ResourceOwnershipQuery;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.BlobAvailability;
import run.ikaros.storage.api.PlacementState;
import run.ikaros.storage.api.StorageTier;

class BlobVerificationServiceTest {
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final AttachmentRepository attachments = mock(AttachmentRepository.class);
    private final ResourceOwnershipQuery resources = mock(ResourceOwnershipQuery.class);
    private final StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
    private final StorageContentReader reader = mock(StorageContentReader.class);
    private final BlobIntegrityService integrity = new Sha256BlobIntegrityService();
    private final TransactionalOperator transaction = mock(TransactionalOperator.class);
    private final DurableEventPublisher events = mock(DurableEventPublisher.class);
    private final BlobVerificationService service = new BlobVerificationService(blobs, placements, attachments,
        resources, providers, List.of(reader), integrity, events, transaction);

    @Test
    void matchingContentIsVerifiedAndRemainsReadable() {
        Fixture fixture = fixture("ikaros");
        when(reader.supports(fixture.provider)).thenReturn(true);
        when(reader.read(fixture.provider, fixture.placement, fixture.blob, null)).thenReturn(Mono.just(content("ikaros")));
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.append(any())).thenReturn(Mono.empty());
        when(placements.save(any(BlobPlacementEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(blobs.save(any(BlobEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.verify(fixture.actorId, fixture.blob.id()))
            .assertNext(view -> assertEquals(BlobIntegrityStatus.VERIFIED, view.status()))
            .verifyComplete();
    }

    @Test
    void corruptContentMarksPlacementUnavailable() {
        Fixture fixture = fixture("expected");
        when(reader.supports(fixture.provider)).thenReturn(true);
        when(reader.read(fixture.provider, fixture.placement, fixture.blob, null)).thenReturn(Mono.just(content("actual")));
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.append(any())).thenReturn(Mono.empty());
        when(placements.save(any(BlobPlacementEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(blobs.save(any(BlobEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.verify(fixture.actorId, fixture.blob.id()))
            .assertNext(view -> assertEquals(BlobIntegrityStatus.CORRUPT, view.status()))
            .verifyComplete();

        org.mockito.ArgumentCaptor<BlobPlacementEntity> captor = org.mockito.ArgumentCaptor.forClass(BlobPlacementEntity.class);
        org.mockito.Mockito.verify(placements).save(captor.capture());
        assertEquals(PlacementState.UNAVAILABLE, captor.getValue().placementState());
    }

    private Fixture fixture(String expectedContent) {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        UUID placementId = UUID.randomUUID();
        Instant now = Instant.now();
        byte[] bytes = expectedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        BlobEntity blob = new BlobEntity(blobId, sha256(bytes), bytes.length,
            "text/plain", BlobAvailability.REMOTE, now, 0L);
        AttachmentEntity attachment = new AttachmentEntity(UUID.randomUUID(), resourceId, blobId, "data.txt",
            AttachmentKind.ORIGINAL, now, null, 0L);
        BlobPlacementEntity placement = new BlobPlacementEntity(placementId, blobId, "archive", StorageTier.ARCHIVE,
            "objects/data.txt", PlacementState.ACTIVE, now, now, 0L);
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "archive", "LOCAL_FILESYSTEM", StorageTier.ARCHIVE,
            StorageProviderStatus.ENABLED, null, java.util.Map.of(), now, now);
        when(blobs.findById(blobId)).thenReturn(Mono.just(blob));
        when(attachments.findFirstByBlobIdAndArchivedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(blobId)).thenReturn(Mono.just(attachment));
        when(resources.requireOwned(actorId, resourceId)).thenReturn(Mono.empty());
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(placement));
        when(providers.getByKey("archive")).thenReturn(Mono.just(provider));
        return new Fixture(actorId, blob, placement, provider);
    }

    private StorageContent content(String text) {
        DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new StorageContent(Flux.just(buffer), "text/plain", buffer.readableByteCount(), buffer.readableByteCount(), 0, buffer.readableByteCount() - 1, false);
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    private record Fixture(UUID actorId, BlobEntity blob, BlobPlacementEntity placement, StorageProvider provider) {}
}
