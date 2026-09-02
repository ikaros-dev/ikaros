package run.ikaros.resource;

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
import run.ikaros.common.PageResponse;

/**
 * 默认 Resource 服务实现，负责维护聚合内一致性并发布可审计的状态变化。
 */
@Service
public class DefaultResourceService implements ResourceService {
    private final ResourceRepository resourceRepository;
    private final ResourceTitleRepository titleRepository;
    private final ExternalIdentityRepository identityRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

    /**
     * 创建 Resource 服务。
     *
     * @param resourceRepository Resource 仓储
     * @param titleRepository 标题仓储
     * @param identityRepository 外部身份仓储
     * @param auditService 审计服务
     * @param transactionalOperator 响应式事务操作器
     */
    public DefaultResourceService(ResourceRepository resourceRepository,
                                  ResourceTitleRepository titleRepository,
                                  ExternalIdentityRepository identityRepository,
                                  AuditService auditService,
                                  TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.titleRepository = titleRepository;
        this.identityRepository = identityRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<ResourceView> create(UUID ownerId, CreateResourceRequest request) {
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(
            null, ownerId, request.type(), ResourceLifecycle.ACTIVE, now, now, null, null
        );
        return transactionalOperator.transactional(resourceRepository.save(resource)
            .flatMap(saved -> titleRepository.save(new ResourceTitleEntity(
                null, saved.id(), request.locale(), request.title(), true, now, now, null
            )).then(auditService.record(ownerId, "resource.create", "RESOURCE", saved.id(), "{}"))
                .then(toView(saved))));
    }

    @Override
    public Mono<ResourceView> get(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId).flatMap(this::toView);
    }

    @Override
    public Mono<PageResponse<ResourceView>> list(UUID ownerId, ResourceType type, String query,
                                                  int page, int size) {
        String typeValue = type == null ? "" : type.name();
        String queryValue = query == null ? "" : query.trim();
        long offset = (long) page * size;
        Mono<List<ResourceView>> items = resourceRepository.search(ownerId, typeValue, queryValue, offset, size)
            .flatMap(this::toView)
            .collectList();
        return Mono.zip(items, resourceRepository.countSearch(ownerId, typeValue, queryValue))
            .map(result -> new PageResponse<>(result.getT1(), result.getT2(), page, size));
    }

    @Override
    public Mono<Void> trash(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .flatMap(resource -> {
                if (resource.lifecycle() == ResourceLifecycle.TRASHED) {
                    return Mono.empty();
                }
                ResourceEntity trashed = new ResourceEntity(
                    resource.id(), resource.ownerId(), resource.resourceType(), ResourceLifecycle.TRASHED,
                    resource.createdAt(), Instant.now(), Instant.now(), resource.version()
                );
                return resourceRepository.save(trashed)
                    .then(auditService.record(ownerId, "resource.trash", "RESOURCE", resourceId, "{}"));
            });
    }

    @Override
    public Mono<ResourceView> restore(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .flatMap(resource -> {
                ResourceEntity restored = new ResourceEntity(
                    resource.id(), resource.ownerId(), resource.resourceType(), ResourceLifecycle.ACTIVE,
                    resource.createdAt(), Instant.now(), null, resource.version()
                );
                return resourceRepository.save(restored)
                    .flatMap(saved -> auditService.record(ownerId, "resource.restore", "RESOURCE", resourceId, "{}")
                        .then(toView(saved)));
            });
    }

    @Override
    public Mono<ExternalIdentityView> addExternalIdentity(UUID ownerId, UUID resourceId,
                                                           CreateExternalIdentityRequest request) {
        Instant now = Instant.now();
        return owned(ownerId, resourceId)
            .then(identityRepository.save(new ExternalIdentityEntity(
                null, resourceId, request.provider(), request.type(), request.value(), now, now, null
            )))
            .onErrorMap(DuplicateKeyException.class,
                exception -> new ConflictException("该外部身份已绑定到其他资源"))
            .flatMap(identity -> auditService.record(ownerId, "resource.external-identity.create",
                    "RESOURCE", resourceId, "{}")
                .thenReturn(toIdentityView(identity)));
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }

    private Mono<ResourceView> toView(ResourceEntity resource) {
        Mono<List<ResourceTitleView>> titles = titleRepository
            .findAllByResourceIdOrderByPrimaryDescLocaleAsc(resource.id())
            .map(title -> new ResourceTitleView(title.id(), title.locale(), title.title(), title.primary()))
            .collectList();
        Mono<List<ExternalIdentityView>> identities = identityRepository
            .findAllByResourceIdOrderByProviderAsc(resource.id())
            .map(this::toIdentityView)
            .collectList();
        return Mono.zip(titles, identities)
            .map(parts -> new ResourceView(resource.id(), resource.resourceType(), resource.lifecycle(),
                parts.getT1(), parts.getT2(), resource.createdAt(), resource.updatedAt()));
    }

    private ExternalIdentityView toIdentityView(ExternalIdentityEntity identity) {
        return new ExternalIdentityView(identity.id(), identity.provider(), identity.externalType(),
            identity.externalId());
    }
}
