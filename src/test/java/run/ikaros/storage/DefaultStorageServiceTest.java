package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;
import run.ikaros.event.DurableEventService;

/**
 * 验证 Attachment、Blob 与 Placement 的存储边界。
 */
class DefaultStorageServiceTest {
    private ResourceRepository resourceRepository;
    private AttachmentRepository attachmentRepository;
    private BlobRepository blobRepository;
    private BlobPlacementRepository placementRepository;
    private DerivedAttachmentRepository derivedAttachmentRepository;
    private AuditService auditService;
    private DefaultStorageService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        attachmentRepository = mock(AttachmentRepository.class);
        blobRepository = mock(BlobRepository.class);
        placementRepository = mock(BlobPlacementRepository.class);
        derivedAttachmentRepository = mock(DerivedAttachmentRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultStorageService(resourceRepository, attachmentRepository, blobRepository,
            placementRepository, derivedAttachmentRepository, auditService, transaction);
    }

    @Test
    void reusesExistingBlobAndCreatesAttachmentWithoutPathCoupling() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.VIDEO,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);
        BlobEntity blob = new BlobEntity(blobId, "a".repeat(64), 2048L, "video/mp4",
            BlobAvailability.AVAILABLE, now, 0L);
        AttachmentEntity attachment = new AttachmentEntity(attachmentId, resourceId, blobId, "episode.mp4",
            AttachmentKind.ORIGINAL, now, null, 0L);
        BlobPlacementEntity placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "nas", StorageTier.WARM,
            "media/episode.mp4", PlacementState.ACTIVE, now, now, 0L);
        AttachBlobRequest request = new AttachBlobRequest("A".repeat(64), 2048L, "video/mp4", "episode.mp4",
            AttachmentKind.ORIGINAL, "nas", StorageTier.WARM, "media/episode.mp4");

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(blobRepository.findBySha256("a".repeat(64))).thenReturn(Mono.just(blob));
        when(placementRepository.findByProviderAndObjectKey("nas", "media/episode.mp4")).thenReturn(Mono.just(placement));
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(Mono.just(attachment));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(placement));
        when(auditService.record(eq(ownerId), eq("attachment.create"), eq("ATTACHMENT"), eq(attachmentId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.attach(ownerId, resourceId, request))
            .assertNext(view -> {
                assertThat(view.blobId()).isEqualTo(blobId);
                assertThat(view.sha256()).isEqualTo("a".repeat(64));
                assertThat(view.placements()).singleElement().satisfies(value -> {
                    assertThat(value.provider()).isEqualTo("nas");
                    assertThat(value.objectKey()).isEqualTo("media/episode.mp4");
                });
            })
            .verifyComplete();

        ArgumentCaptor<AttachmentEntity> attachmentCaptor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().resourceId()).isEqualTo(resourceId);
        assertThat(attachmentCaptor.getValue().blobId()).isEqualTo(blobId);
        verify(blobRepository).findBySha256("a".repeat(64));
    }

    @Test
    void recordsDerivedAttachmentSourceWithoutChangingOriginalKind() {
        UUID ownerId = UUID.randomUUID(); UUID resourceId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID(); UUID derivedId = UUID.randomUUID(); UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.VIDEO, ResourceLifecycle.ACTIVE,
            now, now, null, 0L);
        AttachmentEntity source = new AttachmentEntity(sourceId, resourceId, UUID.randomUUID(), "source.mp4",
            AttachmentKind.ORIGINAL, now, null, 0L);
        BlobEntity blob = new BlobEntity(blobId, "b".repeat(64), 100L, "image/jpeg", BlobAvailability.AVAILABLE, now, 0L);
        AttachmentEntity derived = new AttachmentEntity(derivedId, resourceId, blobId, "cover.jpg",
            AttachmentKind.DERIVED, now, null, 0L);
        BlobPlacementEntity placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "nas", StorageTier.HOT,
            "derived/cover.jpg", PlacementState.ACTIVE, now, now, 0L);
        AttachBlobRequest content = new AttachBlobRequest("B".repeat(64), 100L, "image/jpeg", "cover.jpg",
            AttachmentKind.ORIGINAL, "nas", StorageTier.HOT, "derived/cover.jpg");
        when(attachmentRepository.findById(sourceId)).thenReturn(Mono.just(source));
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(blobRepository.findBySha256("b".repeat(64))).thenReturn(Mono.just(blob));
        when(placementRepository.findByProviderAndObjectKey("nas", "derived/cover.jpg")).thenReturn(Mono.just(placement));
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(Mono.just(derived));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(placement));
        when(derivedAttachmentRepository.save(any())).thenReturn(Mono.just(new DerivedAttachmentEntity(UUID.randomUUID(),
            sourceId, derivedId, now, 0L)));
        when(auditService.record(eq(ownerId), eq("attachment.create"), eq("ATTACHMENT"), eq(derivedId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.attachDerived(ownerId, resourceId,
                new CreateDerivedAttachmentRequest(sourceId, content)))
            .assertNext(view -> assertThat(view.kind()).isEqualTo(AttachmentKind.DERIVED))
            .verifyComplete();
        verify(derivedAttachmentRepository).save(any(DerivedAttachmentEntity.class));
    }

    @Test
    void archivesAttachmentWithoutDeletingItsBlob() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.BOOK,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);
        AttachmentEntity attachment = new AttachmentEntity(attachmentId, resourceId, blobId, "book.pdf",
            AttachmentKind.ORIGINAL, now, null, 0L);
        AttachmentEntity archived = new AttachmentEntity(attachmentId, resourceId, blobId, "book.pdf",
            AttachmentKind.ORIGINAL, now, null, 1L, null, now);
        DurableEventService events = mock(DurableEventService.class);
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(attachmentRepository.findByIdAndResourceIdAndArchivedAtIsNullAndDeletedAtIsNull(
            attachmentId, resourceId)).thenReturn(Mono.just(attachment));
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(Mono.just(archived));
        when(auditService.record(eq(ownerId), eq("attachment.archive"), eq("ATTACHMENT"), eq(attachmentId), eq("{}")))
            .thenReturn(Mono.empty());
        when(events.append(eq("storage.attachment.archived"), eq(1), eq("attachment"), eq(attachmentId), any()))
            .thenReturn(Mono.empty());
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultStorageService eventService = new DefaultStorageService(resourceRepository, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction, null, null, events);

        StepVerifier.create(eventService.archive(ownerId, resourceId, attachmentId)).verifyComplete();
        ArgumentCaptor<AttachmentEntity> capture = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(attachmentRepository).save(capture.capture());
        assertThat(capture.getValue().archivedAt()).isNotNull().isAfterOrEqualTo(now);
        assertThat(capture.getValue().deletedAt()).isNull();
        verify(events).append(eq("storage.attachment.archived"), eq(1), eq("attachment"), eq(attachmentId), any());
    }

    @Test
    void onlyReturnsUnreferencedBlobsOlderThanMinimumAge() {
        Instant oldCreatedAt = Instant.now().minus(Duration.ofDays(3));
        Instant recentCreatedAt = Instant.now().minus(Duration.ofHours(2));
        BlobEntity oldBlob = new BlobEntity(UUID.randomUUID(), "c".repeat(64), 10L, "text/plain",
            BlobAvailability.AVAILABLE, oldCreatedAt, 0L);
        BlobEntity recentBlob = new BlobEntity(UUID.randomUUID(), "d".repeat(64), 20L, "text/plain",
            BlobAvailability.AVAILABLE, recentCreatedAt, 0L);
        when(blobRepository.findGarbageCollectionCandidates()).thenReturn(Flux.just(oldBlob, recentBlob));

        StepVerifier.create(service.findGarbageCollectionCandidates(10, Duration.ofDays(1)))
            .assertNext(candidates -> {
                assertThat(candidates).singleElement().satisfies(candidate -> {
                    assertThat(candidate.blobId()).isEqualTo(oldBlob.id());
                    assertThat(candidate.eligibleAt()).isEqualTo(oldCreatedAt.plus(Duration.ofDays(1)));
                });
            })
            .verifyComplete();
    }

    @Test
    void rejectsNullGarbageCollectionAgeBeforeRepositoryQuery() {
        StepVerifier.create(service.findGarbageCollectionCandidates(10, null))
            .expectError(IllegalArgumentException.class).verify();
        verifyNoInteractions(blobRepository);
    }

    @Test
    void recordsApprovedGarbageCollectionDecision() {
        UUID actorId = UUID.randomUUID();
        BlobEntity blob = new BlobEntity(UUID.randomUUID(), "e".repeat(64), 30L, "text/plain",
            BlobAvailability.AVAILABLE, Instant.now().minus(Duration.ofDays(2)), 0L);
        when(blobRepository.findById(blob.id())).thenReturn(Mono.just(blob));
        when(auditService.record(actorId, "blob.gc.approve", "BLOB", blob.id(), "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.recordGarbageCollectionDecision(actorId, blob.id(), true))
            .verifyComplete();

        verify(auditService).record(actorId, "blob.gc.approve", "BLOB", blob.id(), "{}");
    }
}
