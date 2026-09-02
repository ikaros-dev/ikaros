package run.ikaros.storage;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;

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
    private final StorageProviderRegistry providerRegistry;
    private final BackgroundTaskService taskService;
    private final DurableEventService eventService;
    private StorageContentReader contentReader;

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
        this(resourceRepository, attachmentRepository, blobRepository, placementRepository,
            derivedAttachmentRepository, auditService, transactionalOperator, null, null, null);
    }

    public DefaultStorageService(ResourceRepository resourceRepository,
                                 AttachmentRepository attachmentRepository,
                                 BlobRepository blobRepository,
                                 BlobPlacementRepository placementRepository,
                                 DerivedAttachmentRepository derivedAttachmentRepository,
                                 AuditService auditService,
                                 TransactionalOperator transactionalOperator,
                                 StorageProviderRegistry providerRegistry) {
        this(resourceRepository, attachmentRepository, blobRepository, placementRepository,
            derivedAttachmentRepository, auditService, transactionalOperator, providerRegistry, null, null);
    }

    public DefaultStorageService(ResourceRepository resourceRepository,
                                 AttachmentRepository attachmentRepository,
                                 BlobRepository blobRepository,
                                 BlobPlacementRepository placementRepository,
                                 DerivedAttachmentRepository derivedAttachmentRepository,
                                 AuditService auditService,
                                 TransactionalOperator transactionalOperator,
                                 StorageProviderRegistry providerRegistry,
                                 BackgroundTaskService taskService) {
        this(resourceRepository, attachmentRepository, blobRepository, placementRepository, derivedAttachmentRepository,
            auditService, transactionalOperator, providerRegistry, taskService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultStorageService(ResourceRepository resourceRepository,
                                 AttachmentRepository attachmentRepository,
                                 BlobRepository blobRepository,
                                 BlobPlacementRepository placementRepository,
                                 DerivedAttachmentRepository derivedAttachmentRepository,
                                 AuditService auditService,
                                 TransactionalOperator transactionalOperator,
                                 StorageProviderRegistry providerRegistry,
                                 BackgroundTaskService taskService,
                                 DurableEventService eventService) {
        this.resourceRepository = resourceRepository;
        this.attachmentRepository = attachmentRepository;
        this.blobRepository = blobRepository;
        this.placementRepository = placementRepository;
        this.derivedAttachmentRepository = derivedAttachmentRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
        this.providerRegistry = providerRegistry;
        this.taskService = taskService;
        this.eventService = eventService;
    }

    @Autowired(required = false)
    public void setContentReader(StorageContentReader contentReader) {
        this.contentReader = contentReader;
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
        return attachInternal(ownerId, resourceId, request, null);
    }

    private Mono<AttachmentView> attachInternal(UUID ownerId, UUID resourceId, AttachBlobRequest request,
                                                String idempotencyKey) {
        return owned(ownerId, resourceId).then(transactionalOperator.transactional(
            existingAttachment(resourceId, idempotencyKey)
                .flatMap(existing -> blobRepository.findById(existing.blobId())
                    .switchIfEmpty(Mono.error(new ConflictException("幂等提交引用了不存在的 Blob")))
                    .flatMap(blob -> toView(existing, blob)))
                .switchIfEmpty(Mono.defer(() -> findOrCreateBlob(request)
                    .flatMap(blob -> ensurePlacement(blob, request)
                        .then(markAvailable(blob))
                        .then(attachmentRepository.save(new AttachmentEntity(
                            null, resourceId, blob.id(), request.fileName(), request.kind(), Instant.now(), null, null,
                            idempotencyKey
                        )))
                        .flatMap(attachment -> auditService.record(ownerId, "attachment.create", "ATTACHMENT",
                            attachment.id(), "{}").then(emit("attachment.created", attachment))
                            .then(toView(attachment, blob))))))
        ));
    }

    @Override
    public Mono<AttachmentView> commitUpload(UUID ownerId, UUID resourceId, CommitUploadRequest request) {
        return attachInternal(ownerId, resourceId, request.asAttachment(), request.idempotencyKey());
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
    public Mono<AttachmentView> get(UUID ownerId, UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
            .filter(attachment -> attachment.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(attachment -> owned(ownerId, attachment.resourceId())
                .then(blobRepository.findById(attachment.blobId())
                    .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                    .flatMap(blob -> toView(attachment, blob))));
    }

    @Override
    public Mono<StorageContent> readContent(UUID ownerId, UUID attachmentId, String range) {
        if (contentReader == null || providerRegistry == null) {
            return Mono.error(new ConflictException("Storage Provider 内容读取能力未配置"));
        }
        return attachmentRepository.findById(attachmentId)
            .filter(attachment -> attachment.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(attachment -> owned(ownerId, attachment.resourceId())
                .then(blobRepository.findById(attachment.blobId())
                    .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                    .flatMap(blob -> placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                        .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
                        .next()
                        .switchIfEmpty(Mono.error(new ConflictException("附件当前没有可读副本")))
                        .flatMap(placement -> providerRegistry.getByKey(placement.provider())
                            .flatMap(provider -> contentReader.read(provider, placement, blob, range))))));
    }

    @Override
    public Mono<Void> remove(UUID ownerId, UUID resourceId, UUID attachmentId) {
        return owned(ownerId, resourceId)
            .then(attachmentRepository.findByIdAndResourceIdAndDeletedAtIsNull(attachmentId, resourceId)
                .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
                .flatMap(attachment -> attachmentRepository.save(new AttachmentEntity(
                    attachment.id(), attachment.resourceId(), attachment.blobId(), attachment.fileName(),
                    attachment.attachmentKind(), attachment.createdAt(), Instant.now(), attachment.version()
                )).then(auditService.record(ownerId, "attachment.delete", "ATTACHMENT", attachment.id(), "{}"))
                    .then(emit("attachment.deleted", attachment))))
            .then();
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

    @Override
    public Mono<BackgroundTask> requestGarbageCollection(UUID actorId, int limit, Duration minimumAge) {
        if (taskService == null) {
            return Mono.error(new IllegalStateException("Background Task Runtime 未配置"));
        }
        return taskService.submit("storage.blob-gc", Map.of("limit", limit,
            "minimum_age_seconds", minimumAge.getSeconds(), "requested_by", actorId.toString()),
            "storage.blob-gc:" + actorId + ":" + limit + ":" + minimumAge.getSeconds());
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
                BlobAvailability.PROCESSING, Instant.now(), null
            ))));
    }

    private Mono<AttachmentEntity> existingAttachment(UUID resourceId, String idempotencyKey) {
        return idempotencyKey == null ? Mono.empty()
            : attachmentRepository.findByResourceIdAndIdempotencyKeyAndDeletedAtIsNull(resourceId, idempotencyKey);
    }

    private Mono<Void> ensurePlacement(BlobEntity blob, AttachBlobRequest request) {
        Mono<Void> writable = providerRegistry == null ? Mono.empty()
            : providerRegistry.requireWritableByKey(request.provider())
                .flatMap(provider -> provider.tier() == request.tier() ? Mono.empty()
                    : Mono.error(new ConflictException("Placement tier 与 Storage Provider 配置不一致")));
        return writable.then(placementRepository.findByProviderAndObjectKey(request.provider(), request.objectKey()))
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

    private Mono<Void> markAvailable(BlobEntity blob) {
        if (blob.availability() != BlobAvailability.PROCESSING) {
            return Mono.empty();
        }
        return blobRepository.save(new BlobEntity(blob.id(), blob.hashAlgorithm(), blob.sha256(), blob.sizeBytes(),
            blob.mediaType(), BlobAvailability.AVAILABLE, blob.createdAt(), blob.version())).then();
    }

    private Mono<Void> emit(String eventType, AttachmentEntity attachment) {
        if (eventService == null) {
            return Mono.empty();
        }
        String payload = "{\"attachment_id\":\"" + attachment.id() + "\",\"resource_id\":\""
            + attachment.resourceId() + "\",\"blob_id\":\"" + attachment.blobId() + "\"}";
        return eventService.append(eventType, 1, "attachment", attachment.id(), payload).then();
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
