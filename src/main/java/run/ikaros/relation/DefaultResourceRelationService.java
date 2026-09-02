package run.ikaros.relation;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;

/**
 * 默认关系服务，确保关系两端均属于当前用户且不允许资源自关联。
 */
@Service
public class DefaultResourceRelationService implements ResourceRelationService {
    private final ResourceRepository resourceRepository;
    private final ResourceRelationRepository relationRepository;
    private final AuditService auditService;

    /**
     * 创建 Resource 关系服务。
     *
     * @param resourceRepository Resource 仓储
     * @param relationRepository 关系仓储
     * @param auditService 审计服务
     */
    public DefaultResourceRelationService(ResourceRepository resourceRepository,
                                          ResourceRelationRepository relationRepository, AuditService auditService) {
        this.resourceRepository = resourceRepository;
        this.relationRepository = relationRepository;
        this.auditService = auditService;
    }

    @Override
    public Mono<ResourceRelationView> create(UUID ownerId, UUID sourceResourceId, CreateResourceRelationRequest request) {
        if (sourceResourceId.equals(request.targetResourceId())) {
            return Mono.error(new ConflictException("资源不能与自身建立关系"));
        }
        Instant now = Instant.now();
        return Mono.zip(owned(ownerId, sourceResourceId), owned(ownerId, request.targetResourceId()))
            .then(relationRepository.save(new ResourceRelationEntity(null, sourceResourceId, request.targetResourceId(),
                request.type(), request.position() == null ? 0 : request.position(), now, null)))
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("该资源关系已存在"))
            .flatMap(saved -> auditService.record(ownerId, "resource.relation.create", "RESOURCE", sourceResourceId,
                "{}").thenReturn(toView(saved)));
    }

    @Override
    public Flux<ResourceRelationView> list(UUID ownerId, UUID sourceResourceId) {
        return owned(ownerId, sourceResourceId)
            .thenMany(relationRepository.findAllBySourceResourceIdOrderByRelationTypeAscPositionAsc(sourceResourceId)
                .map(this::toView));
    }

    @Override
    public Mono<Void> remove(UUID ownerId, UUID sourceResourceId, UUID relationId) {
        return owned(ownerId, sourceResourceId)
            .then(relationRepository.findById(relationId))
            .filter(relation -> relation.sourceResourceId().equals(sourceResourceId))
            .switchIfEmpty(Mono.error(new NotFoundException("资源关系不存在")))
            .flatMap(relationRepository::delete)
            .then(auditService.record(ownerId, "resource.relation.delete", "RESOURCE", sourceResourceId, "{}"));
    }

    private Mono<Void> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then();
    }

    private ResourceRelationView toView(ResourceRelationEntity relation) {
        return new ResourceRelationView(relation.id(), relation.targetResourceId(), relation.relationType(),
            relation.position());
    }
}
