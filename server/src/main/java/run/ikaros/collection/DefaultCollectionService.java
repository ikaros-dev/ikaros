package run.ikaros.collection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认 Collection 服务实现，保证集合与资源均属于同一当前拥有者。
 */
@Service
public class DefaultCollectionService implements CollectionService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final CollectionRepository collectionRepository;
    private final CollectionResourceRepository collectionResourceRepository;
    private final ResourceRepository resourceRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;
    private final DurableEventService eventService;

    /**
     * 创建 Collection 服务。
     *
     * @param collectionRepository Collection 仓储
     * @param collectionResourceRepository 成员关系仓储
     * @param resourceRepository Resource 仓储
     * @param auditService 审计服务
     * @param transactionalOperator 响应式事务操作器
     */
    public DefaultCollectionService(CollectionRepository collectionRepository,
                                    CollectionResourceRepository collectionResourceRepository,
                                    ResourceRepository resourceRepository,
                                    AuditService auditService,
                                    TransactionalOperator transactionalOperator) {
        this(collectionRepository, collectionResourceRepository, resourceRepository, auditService,
            transactionalOperator, null);
    }

    @Autowired
    public DefaultCollectionService(CollectionRepository collectionRepository,
                                    CollectionResourceRepository collectionResourceRepository,
                                    ResourceRepository resourceRepository,
                                    AuditService auditService,
                                    TransactionalOperator transactionalOperator,
                                    DurableEventService eventService) {
        this.collectionRepository = collectionRepository;
        this.collectionResourceRepository = collectionResourceRepository;
        this.resourceRepository = resourceRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
        this.eventService = eventService;
    }

    @Override
    public Mono<CollectionView> create(UUID ownerId, CreateCollectionRequest request) {
        Instant now = Instant.now();
        return transactionalOperator.transactional(parent(ownerId, request.parentId()).then(collectionRepository.save(new CollectionEntity(
                null, ownerId, request.parentId(), request.name(), request.description(),
                now, now, null)))
            .flatMap(collection -> emitCreated(collection)
                .then(auditService.record(ownerId, "collection.create", "COLLECTION", collection.id(), "{}"))
                .thenReturn(toView(collection))));
    }

    @Override
    public Mono<List<CollectionView>> list(UUID ownerId) {
        return collectionRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId)
            .take(MAX_UNPAGED_RESULTS).map(this::toView)
            .collectList();
    }

    @Override
    public Mono<PageResponse<CollectionView>> list(UUID ownerId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        return collectionRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId)
            .take(MAX_UNPAGED_RESULTS)
            .map(this::toView)
            .collectList()
            .map(all -> new PageResponse<>(all.stream().skip((long) page * size).limit(size).toList(),
                all.size(), page, size));
    }

    @Override
    public Mono<Void> addResource(UUID ownerId, UUID collectionId, UUID resourceId, int position) {
        return transactionalOperator.transactional(ownedCollection(ownerId, collectionId)
            .then(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问"))))
            .then(collectionResourceRepository.save(new CollectionResourceEntity(
                null, collectionId, resourceId, position, Instant.now(), null
            )))
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("资源已在该集合中"))
            .then(emitMembership("resource.collection.member-added", collectionId, resourceId))
            .then(auditService.record(ownerId, "collection.resource.add", "COLLECTION", collectionId,
                "{\"resourceId\":\"" + resourceId + "\"}")));
    }

    @Override
    public Mono<CollectionView> move(UUID ownerId, UUID collectionId, UUID parentId) {
        return transactionalOperator.transactional(ownedCollection(ownerId, collectionId)
            .flatMap(collection -> parent(ownerId, parentId)
                .then(validateNoCycle(collection.id(), parentId))
                .then(Mono.defer(() -> collectionRepository.save(new CollectionEntity(
                    collection.id(), collection.ownerId(), parentId, collection.name(), collection.description(),
                    collection.createdAt(), Instant.now(), collection.version())))))
            .map(this::toView));
    }

    @Override
    public Mono<Void> removeResource(UUID ownerId, UUID collectionId, UUID resourceId) {
        return transactionalOperator.transactional(ownedCollection(ownerId, collectionId)
            .then(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问"))))
            .then(collectionResourceRepository.deleteByCollectionIdAndResourceId(collectionId, resourceId))
            .then(emitMembership("resource.collection.member-removed", collectionId, resourceId))
            .then(auditService.record(ownerId, "collection.resource.remove", "COLLECTION", collectionId,
                "{\"resourceId\":\"" + resourceId + "\"}")));
    }

    private Mono<Void> parent(UUID ownerId, UUID parentId) {
        if (parentId == null) {
            return Mono.empty();
        }
        return ownedCollection(ownerId, parentId).then();
    }

    private Mono<Void> validateNoCycle(UUID collectionId, UUID parentId) {
        if (parentId == null) {
            return Mono.empty();
        }
        if (collectionId.equals(parentId)) {
            return Mono.error(new ConflictException("集合不能将自身作为父集合"));
        }
        return collectionRepository.findById(parentId)
            .flatMap(parent -> validateNoCycle(collectionId, parent.parentId()))
            .switchIfEmpty(Mono.error(new NotFoundException("父集合不存在")))
            .then();
    }

    private Mono<CollectionEntity> ownedCollection(UUID ownerId, UUID collectionId) {
        return collectionRepository.findByIdAndOwnerId(collectionId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("集合不存在或无权访问")));
    }

    private CollectionView toView(CollectionEntity collection) {
        return new CollectionView(collection.id(), collection.parentId(), collection.name(), collection.description(), collection.createdAt(),
            collection.updatedAt());
    }

    private Mono<Void> emitCreated(CollectionEntity collection) {
        if (eventService == null) return Mono.empty();
        String payload = "{\"collection_id\":\"" + collection.id()
            + "\",\"kind\":\"library\",\"mode\":\"STATIC\"}";
        return eventService.append("resource.collection.created", 1, "collection", collection.id(), payload).then();
    }

    private Mono<Void> emitMembership(String eventType, UUID collectionId, UUID resourceId) {
        if (eventService == null) return Mono.empty();
        String payload = "{\"collection_id\":\"" + collectionId + "\",\"resource_id\":\"" + resourceId + "\"}";
        return eventService.append(eventType, 1, "collection", collectionId, payload).then();
    }
}
