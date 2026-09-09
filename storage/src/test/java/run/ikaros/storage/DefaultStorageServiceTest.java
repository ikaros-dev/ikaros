package run.ikaros.storage;

import run.ikaros.storage.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.PageResponse;
import run.ikaros.resource.api.ResourceOwnershipQuery;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;

/**
 * 验证 Attachment、Blob 与 Placement 的存储边界。
 */
class DefaultStorageServiceTest {
    private ResourceOwnershipQuery resourceOwnership;
    private AttachmentRepository attachmentRepository;
    private BlobRepository blobRepository;
    private BlobPlacementRepository placementRepository;
    private DerivedAttachmentRepository derivedAttachmentRepository;
    private AuditService auditService;
    private DefaultStorageService service;

    @BeforeEach
    void setUp() {
        resourceOwnership = mock(ResourceOwnershipQuery.class);
        attachmentRepository = mock(AttachmentRepository.class);
        blobRepository = mock(BlobRepository.class);
        placementRepository = mock(BlobPlacementRepository.class);
        derivedAttachmentRepository = mock(DerivedAttachmentRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultStorageService(resourceOwnership, attachmentRepository, blobRepository,
            placementRepository, derivedAttachmentRepository, auditService, transaction);
    }

    @Test
    void reusesExistingBlobAndCreatesAttachmentWithoutPathCoupling() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        BlobEntity blob = new BlobEntity(blobId, "a".repeat(64), 2048L, "video/mp4",
            BlobAvailability.AVAILABLE, now, 0L);
        AttachmentEntity attachment = new AttachmentEntity(attachmentId, resourceId, blobId, "episode.mp4",
            AttachmentKind.ORIGINAL, now, null, 0L);
        BlobPlacementEntity placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "nas", StorageTier.WARM,
            "media/episode.mp4", PlacementState.ACTIVE, now, now, 0L);
        AttachBlobRequest request = new AttachBlobRequest("A".repeat(64), 2048L, "video/mp4", "episode.mp4",
            AttachmentKind.ORIGINAL, "nas", StorageTier.WARM, "media/episode.mp4");

        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
        when(blobRepository.findBySha256("a".repeat(64))).thenReturn(Mono.just(blob));
        when(placementRepository.findByProviderAndObjectKey("nas", "media/episode.mp4")).thenReturn(Mono.just(placement));
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(Mono.just(attachment));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(placement));
        when(auditService.record(eq(ownerId), eq("attachment.create"), eq("ATTACHMENT"), eq(attachmentId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.attach(ownerId, resourceId, request))
            .assertNext(view -> {
                assertThat(view.resourceId()).isEqualTo(resourceId);
                assertThat(view.sha256()).isEqualTo("a".repeat(64));
                assertThat(view.availability()).isEqualTo(AttachmentAvailabilityStatus.READY);
            })
            .verifyComplete();

        ArgumentCaptor<AttachmentEntity> attachmentCaptor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().resourceId()).isEqualTo(resourceId);
        assertThat(attachmentCaptor.getValue().blobId()).isEqualTo(blobId);
        verify(blobRepository).findBySha256("a".repeat(64));
    }

    @Test
    void rejectsSameHashWithDifferentSizeBeforeCreatingAttachment() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        BlobEntity existing = new BlobEntity(UUID.randomUUID(), "a".repeat(64), 2048L, "video/mp4",
            BlobAvailability.AVAILABLE, now, 0L);
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
        when(blobRepository.findBySha256("a".repeat(64))).thenReturn(Mono.just(existing));

        StepVerifier.create(service.attach(ownerId, resourceId, new AttachBlobRequest("A".repeat(64), 1024L,
                "video/mp4", "episode.mp4", AttachmentKind.ORIGINAL, "nas", StorageTier.WARM, "episode.mp4")))
            .expectErrorMessage("相同 SHA-256 的 Blob 大小不一致").verify();
        verify(blobRepository, org.mockito.Mockito.never()).save(any(BlobEntity.class));
        verify(attachmentRepository, org.mockito.Mockito.never()).save(any(AttachmentEntity.class));
    }

    @Test
    void retriesCommitWithSameIdempotencyKeyWithoutCreatingAnotherAttachment() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction,
            providers, null, null);
        uploadService.setObjectProviderRegistry(objects);
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "local", "local", StorageTier.WARM,
            StorageProviderStatus.ENABLED, null, java.util.Map.of(), now, now);
        AttachmentEntity existing = new AttachmentEntity(attachmentId, resourceId, blobId, "a.bin",
            AttachmentKind.ORIGINAL, now, null, 0L, "retry-key");
        BlobEntity blob = new BlobEntity(blobId, "a".repeat(64), 10L, "application/octet-stream",
            BlobAvailability.AVAILABLE, now, 0L);
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
        when(providers.requireWritableByKey("local")).thenReturn(Mono.just(provider));
        when(objects.verify(provider, "a.bin")).thenReturn(Mono.just(new StorageObjectMetadata(
            "a.bin", 10L, "application/octet-stream", "etag")));
        when(attachmentRepository.findByResourceIdAndIdempotencyKeyAndArchivedAtIsNullAndDeletedAtIsNull(
            resourceId, "retry-key")).thenReturn(Mono.just(existing));
        when(blobRepository.findById(blobId)).thenReturn(Mono.just(blob));

        CommitUploadRequest request = new CommitUploadRequest("a".repeat(64), "a".repeat(64), false, 10L,
            "application/octet-stream", "a.bin", AttachmentKind.ORIGINAL, "local", StorageTier.WARM,
            "a.bin", "retry-key");
        StepVerifier.create(uploadService.commitUpload(ownerId, resourceId, request))
            .assertNext(view -> assertThat(view.id()).isEqualTo(attachmentId))
            .verifyComplete();
        verify(attachmentRepository, org.mockito.Mockito.never()).save(any(AttachmentEntity.class));
        verify(blobRepository, org.mockito.Mockito.never()).save(any(BlobEntity.class));
    }

    @Test
    void abortsOwnedOpenUploadSessionIdempotently() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction,
            providers, null, null, sessions);
        uploadService.setObjectProviderRegistry(objects);
        UploadSessionEntity open = new UploadSessionEntity(sessionId, ownerId, resourceId, "local", "tmp/a.bin",
            10L, "a".repeat(64), UploadSessionState.OPEN, now.plusSeconds(600), now, now, 0L, "key");
        UploadSessionEntity aborted = new UploadSessionEntity(sessionId, ownerId, resourceId, "local", "tmp/a.bin",
            10L, "a".repeat(64), UploadSessionState.ABORTED, open.expiresAt(), now, now.plusSeconds(1), 1L, "key");
        when(sessions.findByIdAndOwnerId(sessionId, ownerId)).thenReturn(Mono.just(open));
        when(sessions.save(any(UploadSessionEntity.class))).thenReturn(Mono.just(aborted));
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "local", "local", StorageTier.WARM,
            StorageProviderStatus.ENABLED, null, java.util.Map.of(), now, now);
        when(providers.getByKey("local")).thenReturn(Mono.just(provider));
        when(objects.deleteObject(provider, "tmp/a.bin")).thenReturn(Mono.empty());

        StepVerifier.create(uploadService.abortUploadSession(ownerId, sessionId))
            .assertNext(view -> assertThat(view.state()).isEqualTo(UploadSessionState.ABORTED))
            .verifyComplete();
        verify(sessions).save(argThat(value -> value.state() == UploadSessionState.ABORTED));
        verify(objects).deleteObject(provider, "tmp/a.bin");
    }

    @Test
    void persistsSessionAndReturnsItsIdWhenBeginningUpload() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction,
            providers, null, null, sessions);
        uploadService.setObjectProviderRegistry(objects);
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "local", "local", StorageTier.WARM,
            StorageProviderStatus.ENABLED, null, java.util.Map.of(), now, now);
        UUID sessionId = UUID.randomUUID();
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
        when(providers.requireWritableByKey("local")).thenReturn(Mono.just(provider));
        when(blobRepository.findBySha256("a".repeat(64))).thenReturn(Mono.empty());
        when(objects.createUploadIntent(eq(provider), any(StorageUploadRequest.class))).thenReturn(Mono.just(
            new StorageUploadIntent("PUT", "https://upload.example", "attachments/a.bin", now.plusSeconds(600))));
        when(sessions.save(any(UploadSessionEntity.class))).thenReturn(Mono.just(new UploadSessionEntity(sessionId,
            ownerId, resourceId, "local", "attachments/a.bin", 10L, "a".repeat(64), UploadSessionState.OPEN,
            now.plusSeconds(600), now, now, 0L, "key")));

        StepVerifier.create(uploadService.beginUpload(ownerId, resourceId,
                new BeginUploadRequest("a.bin", 10L, "application/octet-stream", "local", null, "a".repeat(64)),
            "key"))
            .assertNext(view -> {
                assertThat(view.sessionId()).isEqualTo(sessionId);
                assertThat(view.deduplicated()).isFalse();
            }).verifyComplete();
        verify(sessions).save(argThat(session -> session.state() == UploadSessionState.OPEN
            && session.idempotencyKey().equals("key")));
    }

    @Test
    void recordsDerivedAttachmentSourceWithoutChangingOriginalKind() {
        UUID ownerId = UUID.randomUUID(); UUID resourceId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID(); UUID derivedId = UUID.randomUUID(); UUID blobId = UUID.randomUUID();
        Instant now = Instant.now();
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
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
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
        AttachmentEntity attachment = new AttachmentEntity(attachmentId, resourceId, blobId, "book.pdf",
            AttachmentKind.ORIGINAL, now, null, 0L);
        AttachmentEntity archived = new AttachmentEntity(attachmentId, resourceId, blobId, "book.pdf",
            AttachmentKind.ORIGINAL, now, null, 1L, null, now);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.empty());
        when(attachmentRepository.findByIdAndResourceIdAndArchivedAtIsNullAndDeletedAtIsNull(
            attachmentId, resourceId)).thenReturn(Mono.just(attachment));
        when(attachmentRepository.save(any(AttachmentEntity.class))).thenReturn(Mono.just(archived));
        when(auditService.record(eq(ownerId), eq("attachment.archive"), eq("ATTACHMENT"), eq(attachmentId), eq("{}")))
            .thenReturn(Mono.empty());
        when(events.append(any(EventAppendRequest.class)))
            .thenReturn(Mono.empty());
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DefaultStorageService eventService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction, null, null, events);

        StepVerifier.create(eventService.archive(ownerId, resourceId, attachmentId)).verifyComplete();
        ArgumentCaptor<AttachmentEntity> capture = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(attachmentRepository).save(capture.capture());
        assertThat(capture.getValue().archivedAt()).isNotNull().isAfterOrEqualTo(now);
        assertThat(capture.getValue().deletedAt()).isNull();
        verify(events).append(argThat(request -> request.eventType().equals("storage.attachment.archived")
            && request.producerSubsystem().equals("storage") && request.subjectType().equals("attachment")
            && request.subjectId().equals(attachmentId)));
    }

    @Test
    void listsAllOwnerAttachmentsWithPaginationWhenResourceFilterIsMissing() {
        UUID ownerId = UUID.randomUUID();
        UUID firstResourceId = UUID.randomUUID();
        UUID secondResourceId = UUID.randomUUID();
        Instant now = Instant.now();
        AttachmentEntity first = new AttachmentEntity(UUID.randomUUID(), firstResourceId, UUID.randomUUID(),
            "first.txt", AttachmentKind.ORIGINAL, now, null, 0L);
        AttachmentEntity second = new AttachmentEntity(UUID.randomUUID(), secondResourceId, UUID.randomUUID(),
            "second.txt", AttachmentKind.SUBTITLE, now.plusSeconds(1), null, 0L);
        BlobEntity firstBlob = new BlobEntity(first.blobId(), "f".repeat(64), 10L, "text/plain",
            BlobAvailability.AVAILABLE, now, 0L);
        BlobEntity secondBlob = new BlobEntity(second.blobId(), "s".repeat(64), 20L, "text/plain",
            BlobAvailability.AVAILABLE, now, 0L);
        when(attachmentRepository.search(ownerId, null, 0, 20)).thenReturn(Flux.just(first, second));
        when(attachmentRepository.countSearch(ownerId, null)).thenReturn(Mono.just(2L));
        when(blobRepository.findById(first.blobId())).thenReturn(Mono.just(firstBlob));
        when(blobRepository.findById(second.blobId())).thenReturn(Mono.just(secondBlob));
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(first.blobId())).thenReturn(Flux.empty());
        when(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(second.blobId())).thenReturn(Flux.empty());

        StepVerifier.create(service.listPage(ownerId, null, 0, 20))
            .assertNext(page -> {
                assertThat(page).isEqualTo(new PageResponse<>(
                    List.of(new AttachmentView(first.id(), first.resourceId(), first.fileName(), first.attachmentKind(),
                            firstBlob.sha256(), firstBlob.sizeBytes(), firstBlob.mediaType(), AttachmentAvailabilityStatus.READY),
                        new AttachmentView(second.id(), second.resourceId(), second.fileName(), second.attachmentKind(),
                            secondBlob.sha256(), secondBlob.sizeBytes(), secondBlob.mediaType(), AttachmentAvailabilityStatus.READY)),
                    2, 0, 20));
            })
            .verifyComplete();
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
    void rejectsUnknownResourceBeforeCreatingUploadIntent() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService, transaction,
            providers, null, null);
        uploadService.setObjectProviderRegistry(objects);
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.error(
            new run.ikaros.common.NotFoundException("资源不存在或无权访问")));

        StepVerifier.create(uploadService.beginUpload(ownerId, resourceId,
                new BeginUploadRequest("book.pdf", 10, "application/pdf", "local", null, "a".repeat(64))))
            .expectErrorMessage("资源不存在或无权访问").verify();
        verify(providers, never()).requireWritableByKey("local");
        verifyNoInteractions(blobRepository);
    }

    @Test
    void rejectsInvalidUploadConstraintsBeforeProviderAccess() {
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService,
            mock(TransactionalOperator.class), providers, null, null);
        uploadService.setObjectProviderRegistry(objects);

        StepVerifier.create(uploadService.beginUpload(UUID.randomUUID(), UUID.randomUUID(),
                new BeginUploadRequest("book.pdf", -1, "application/pdf", "local", null, "bad")))
            .expectErrorMessage("上传大小不能为负数").verify();
        verifyNoInteractions(providers, objects);
    }

    @Test
    void rejectsUnknownResourceBeforeVerifyingUploadedObject() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        DefaultStorageService uploadService = new DefaultStorageService(resourceOwnership, attachmentRepository,
            blobRepository, placementRepository, derivedAttachmentRepository, auditService,
            mock(TransactionalOperator.class), providers, null, null);
        uploadService.setObjectProviderRegistry(objects);
        when(resourceOwnership.requireOwned(ownerId, resourceId)).thenReturn(Mono.error(
            new run.ikaros.common.NotFoundException("资源不存在或无权访问")));

        StepVerifier.create(uploadService.commitUpload(ownerId, resourceId,
                new CommitUploadRequest("a".repeat(64), 10, "application/octet-stream", "a.bin",
                    AttachmentKind.ORIGINAL, "local", StorageTier.WARM, "a.bin")))
            .expectErrorMessage("资源不存在或无权访问").verify();
        verifyNoInteractions(providers, objects, attachmentRepository, blobRepository, placementRepository);
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
