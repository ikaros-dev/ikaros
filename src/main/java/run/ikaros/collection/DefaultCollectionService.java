package run.ikaros.collection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认 Collection 服务实现，保证集合与资源均属于同一当前拥有者。
 */
@Service
public class DefaultCollectionService implements CollectionService {
    private final CollectionRepository collectionRepository;
    private final CollectionResourceRepository collectionResourceRepository;
    private final ResourceRepository resourceRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

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
        this.collectionRepository = collectionRepository;
        this.collectionResourceRepository = collectionResourceRepository;
        this.resourceRepository = resourceRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<CollectionView> create(UUID ownerId, CreateCollectionRequest request) {
        Instant now = Instant.now();
        return collectionRepository.save(new CollectionEntity(null, ownerId, request.name(), request.description(),
                now, now, null))
            .flatMap(collection -> auditService.record(ownerId, "collection.create", "COLLECTION", collection.id(), "{}")
                .thenReturn(toView(collection)));
    }

    @Override
    public Mono<List<CollectionView>> list(UUID ownerId) {
        return collectionRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId)
            .map(this::toView)
            .collectList();
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
            .then(auditService.record(ownerId, "collection.resource.add", "COLLECTION", collectionId,
                "{\"resourceId\":\"" + resourceId + "\"}")));
    }

    private Mono<CollectionEntity> ownedCollection(UUID ownerId, UUID collectionId) {
        return collectionRepository.findByIdAndOwnerId(collectionId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("集合不存在或无权访问")));
    }

    private CollectionView toView(CollectionEntity collection) {
        return new CollectionView(collection.id(), collection.name(), collection.description(), collection.createdAt(),
            collection.updatedAt());
    }
}
