package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.NotFoundException;

/**
 * 默认收藏服务实现，收藏关系按用户隔离并写入审计记录。
 */
@Service
public class DefaultFavoriteService implements FavoriteService {
    private final ResourceRepository resourceRepository;
    private final FavoriteRepository favoriteRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

    /**
     * 创建收藏服务。
     *
     * @param resourceRepository Resource 仓储
     * @param favoriteRepository 收藏关系仓储
     * @param auditService 审计服务
     * @param transactionalOperator 响应式事务操作器
     */
    public DefaultFavoriteService(ResourceRepository resourceRepository, FavoriteRepository favoriteRepository,
                                  AuditService auditService, TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.favoriteRepository = favoriteRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<FavoriteView> add(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .then(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)
                .map(existing -> new FavoriteView(resourceId, true))
                .switchIfEmpty(Mono.defer(() -> favoriteRepository.save(new FavoriteEntity(null, ownerId, resourceId,
                    Instant.now(), null))
                    .flatMap(saved -> auditService.record(ownerId, "resource.favorite.add", "RESOURCE", resourceId, "{}")
                        .thenReturn(new FavoriteView(saved.resourceId(), true))))))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> remove(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .then(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)
                .flatMap(existing -> favoriteRepository.deleteByOwnerIdAndResourceId(ownerId, resourceId)
                    .then(auditService.record(ownerId, "resource.favorite.remove", "RESOURCE", resourceId, "{}")))
                .then())
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<FavoriteView> get(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .then(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)
                .map(existing -> new FavoriteView(resourceId, true))
                .defaultIfEmpty(new FavoriteView(resourceId, false)));
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }
}
