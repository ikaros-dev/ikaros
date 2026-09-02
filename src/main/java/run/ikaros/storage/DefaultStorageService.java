package run.ikaros.storage;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认存储服务实现，严格保持 Attachment、Blob 与物理 Placement 三层分离。
 */
@Service
public class DefaultStorageService implements StorageService {
    private final ResourceRepository resourceRepository;
    private final AttachmentRepository attachmentRepository;
    private final BlobRepository blobRepository;
    private final BlobPlacementRepository placementRepository;
    private final DerivedAttachmentRepository derivedAttachmentRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

    /**
     * 创建存储服务。
     *
     * @param resourceRepository Resource 仓储
     * @param attachmentRepository Attachment 仓储
     * @param blobRepository Blob 仓储
     * @param placementRepository Placement 仓储
     * @param auditService 审计服务
     * @param transactionalOperator 响应式事务操作器
     */
    public DefaultStorageService(ResourceRepository resourceRepository,
                                 AttachmentRepository attachmentRepository,
                                 BlobRepository blobRepository,
                                 BlobPlacementRepository placementRepository,
                                 DerivedAttachmentRepository derivedAttachmentRepository,
                                 AuditService auditService,
                                 TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.attachmentRepository = attachmentRepository;
        this.blobRepository = blobRepository;
        this.placementRepository = placementRepository;
        this.derivedAttachmentRepository = derivedAttachmentRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<AttachmentView> attachDerived(UUID ownerId, UUID resourceId, CreateDerivedAttachmentRequest request) {
        return attachmentRepository.findById(request.sourceAttachmentId())
            .filter(source -> source.resourceId().equals(resourceId) && source.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("来源附件不存在或无权访问")))
            .then(attach(ownerId, resourceId, new AttachBlobRequest(request.content().sha256(), request.content().sizeBytes(),
                request.content().mediaType(), request.content().fileName(), AttachmentKind.DERIVED,
                request.content().provider(), request.content().tier(), request.content().objectKey())))
            .flatMap(view -> derivedAttachmentRepository.save(new DerivedAttachmentEntity(null, request.sourceAttachmentId(),
                view.id(), Instant.now(), null)).thenReturn(view));
    }

    @Override
    public Mono<AttachmentView> attach(UUID ownerId, UUID resourceId, AttachBlobRequest request) {
        return owned(ownerId, resourceId).then(transactionalOperator.transactional(
            findOrCreateBlob(request)
                .flatMap(blob -> ensurePlacement(blob, request)
                    .then(attachmentRepository.save(new AttachmentEntity(
                        null, resourceId, blob.id(), request.fileName(), request.kind(), Instant.now(), null, null
                    )))
                    .flatMap(attachment -> auditService.record(ownerId, "attachment.create", "ATTACHMENT",
                        attachment.id(), "{}").then(toView(attachment, blob)))
                )
        ));
    }

    @Override
    public Mono<List<AttachmentView>> list(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .thenMany(attachmentRepository.findAllByResourceIdAndDeletedAtIsNullOrderByCreatedAtAsc(resourceId))
            .flatMap(attachment -> blobRepository.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                .flatMap(blob -> toView(attachment, blob)))
            .collectList();
    }

    @Override
    public Mono<List<BlobGcCandidateView>> findGarbageCollectionCandidates(int limit, Duration minimumAge) {
        if (limit < 1 || limit > 500) {
            return Mono.error(new IllegalArgumentException("GC 候选数量必须介于 1 和 500 之间"));
        }
        if (minimumAge.isNegative()) {
            return Mono.error(new IllegalArgumentException("GC 最小保留期不能为负数"));
        }
        Instant eligibleBefore = Instant.now().minus(minimumAge);
        return blobRepository.findGarbageCollectionCandidates()
            .filter(blob -> !blob.createdAt().isAfter(eligibleBefore))
            .take(limit)
            .map(blob -> new BlobGcCandidateView(blob.id(), blob.sha256(), blob.sizeBytes(), blob.createdAt(),
                blob.createdAt().plus(minimumAge)))
            .collectList();
    }

    @Override
    public Mono<Void> recordGarbageCollectionDecision(UUID actorId, UUID blobId, boolean approved) {
        return blobRepository.findById(blobId)
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> auditService.record(actorId, approved ? "blob.gc.approve" : "blob.gc.reject",
                "BLOB", blob.id(), "{}"));
    }

    private Mono<BlobEntity> findOrCreateBlob(AttachBlobRequest request) {
        return blobRepository.findBySha256(request.sha256().toLowerCase())
            .flatMap(existing -> {
                if (!"SHA-256".equalsIgnoreCase(existing.hashAlgorithm())
                    || existing.sizeBytes() != request.sizeBytes()) {
                    return Mono.error(new ConflictException("相同 SHA-256 的 Blob 大小不一致"));
                }
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> blobRepository.save(new BlobEntity(
                null, "SHA-256", request.sha256().toLowerCase(), request.sizeBytes(), request.mediaType(),
                BlobAvailability.AVAILABLE, Instant.now(), null
            ))));
    }

    private Mono<Void> ensurePlacement(BlobEntity blob, AttachBlobRequest request) {
        return placementRepository.findByProviderAndObjectKey(request.provider(), request.objectKey())
            .flatMap(existing -> {
                if (!existing.blobId().equals(blob.id())) {
                    return Mono.error(new ConflictException("该存储对象键已指向其他 Blob"));
                }
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> placementRepository.save(new BlobPlacementEntity(
                null, blob.id(), request.provider(), request.tier(), request.objectKey(),
                PlacementState.ACTIVE, Instant.now(), Instant.now(), null
            ))))
            .then();
    }

    private Mono<AttachmentView> toView(AttachmentEntity attachment, BlobEntity blob) {
        return placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
            .map(placement -> new PlacementView(placement.id(), placement.provider(), placement.storageTier(),
                placement.objectKey(), placement.placementState()))
            .collectList()
            .map(placements -> new AttachmentView(attachment.id(), attachment.fileName(), attachment.attachmentKind(),
                blob.id(), blob.sha256(), blob.sizeBytes(), blob.mediaType(), blob.availability(), placements));
    }

    private Mono<Void> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then();
    }
}
