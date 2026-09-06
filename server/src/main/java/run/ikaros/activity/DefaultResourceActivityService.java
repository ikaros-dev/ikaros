package run.ikaros.activity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认 Resource Activity 服务实现，活动数据与不可删除的审计数据严格分离。
 */
@Service
public class DefaultResourceActivityService implements ResourceActivityService {
    private final ResourceRepository resourceRepository;
    private final ResourceActivityRepository activityRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

    /** 创建 Resource Activity 服务。 */
    public DefaultResourceActivityService(ResourceRepository resourceRepository,
                                          ResourceActivityRepository activityRepository,
                                          AuditService auditService,
                                          TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.activityRepository = activityRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<ResourceActivityView> record(UUID ownerId, UUID resourceId, RecordActivityRequest request) {
        return owned(ownerId, resourceId)
            .then(activityRepository.save(new ResourceActivityEntity(null, ownerId, resourceId, request.type(),
                request.details() == null ? "{}" : request.details(), Instant.now(), null)))
            .map(this::toView)
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<List<ResourceActivityView>> recent(UUID ownerId, int limit) {
        if (limit < 1 || limit > 200) {
            return Mono.error(new IllegalArgumentException("Activity 查询数量必须介于 1 和 200 之间"));
        }
        return activityRepository.findAllByOwnerIdOrderByOccurredAtDesc(ownerId)
            .take(limit)
            .map(this::toView)
            .collectList();
    }

    @Override
    public Mono<Void> delete(UUID ownerId, UUID activityId) {
        return activityRepository.findByIdAndOwnerId(activityId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Activity 不存在或无权访问")))
            .flatMap(activity -> activityRepository.deleteById(activity.id())
                .then(auditService.record(ownerId, "resource.activity.delete", "RESOURCE_ACTIVITY", activity.id(), "{}")))
            .as(transactionalOperator::transactional);
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }

    private ResourceActivityView toView(ResourceActivityEntity activity) {
        return new ResourceActivityView(activity.id(), activity.resourceId(), activity.activityType(),
            activity.details(), activity.occurredAt());
    }
}
