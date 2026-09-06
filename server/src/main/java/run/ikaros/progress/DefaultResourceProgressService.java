package run.ikaros.progress;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认消费进度服务，使用用户、资源、进度类型三元组保证幂等更新。
 */
@Service
public class DefaultResourceProgressService implements ResourceProgressService {
    private final ResourceRepository resourceRepository;
    private final ResourceProgressRepository progressRepository;
    private final TransactionalOperator transactionalOperator;

    /** 创建消费进度服务。 */
    public DefaultResourceProgressService(ResourceRepository resourceRepository,
                                          ResourceProgressRepository progressRepository,
                                          TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.progressRepository = progressRepository;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<ResourceProgressView> set(UUID ownerId, UUID resourceId, SetProgressRequest request) {
        return setInternal(ownerId, resourceId, request, null);
    }

    @Override
    public Mono<ResourceProgressView> set(UUID ownerId, UUID resourceId, SetProgressRequest request,
                                          long expectedVersion) {
        return setInternal(ownerId, resourceId, request, expectedVersion);
    }

    private Mono<ResourceProgressView> setInternal(UUID ownerId, UUID resourceId, SetProgressRequest request,
                                                   Long expectedVersion) {
        if (request.type() == null) {
            return Mono.error(new IllegalArgumentException("进度类型不能为空"));
        }
        if (request.total() != null && request.position() > request.total()) {
            return Mono.error(new IllegalArgumentException("当前进度不能超过总进度"));
        }
        return owned(ownerId, resourceId)
            .then(progressRepository.findByOwnerIdAndResourceIdAndProgressType(ownerId, resourceId, request.type())
                .flatMap(existing -> {
                    long actualVersion = existing.version() == null ? 0 : existing.version();
                    if (expectedVersion != null && actualVersion != expectedVersion) {
                        return Mono.error(new PreconditionFailedException("If-Match 与消费进度当前版本不匹配"));
                    }
                    return progressRepository.save(new ResourceProgressEntity(existing.id(), ownerId, resourceId,
                        request.type(), request.position(), request.total(), request.completed(), Instant.now(), existing.version()));
                })
                .switchIfEmpty(Mono.defer(() -> progressRepository.save(new ResourceProgressEntity(null, ownerId, resourceId,
                    request.type(), request.position(), request.total(), request.completed(), Instant.now(), null)))))
            .map(this::toView)
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<ResourceProgressView> get(UUID ownerId, UUID resourceId, ProgressType type) {
        if (type == null) {
            return Mono.error(new IllegalArgumentException("进度类型不能为空"));
        }
        return owned(ownerId, resourceId)
            .then(progressRepository.findByOwnerIdAndResourceIdAndProgressType(ownerId, resourceId, type)
                .switchIfEmpty(Mono.error(new NotFoundException("消费进度不存在"))))
            .map(this::toView);
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }

    private ResourceProgressView toView(ResourceProgressEntity progress) {
        return new ResourceProgressView(progress.id(), progress.resourceId(), progress.progressType(),
            progress.positionValue(), progress.totalValue(), progress.completed(), progress.updatedAt(), progress.version());
    }
}
