package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
}
