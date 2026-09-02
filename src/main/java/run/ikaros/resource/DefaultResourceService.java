package run.ikaros.resource;

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
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;
import run.ikaros.event.DurableEventService;

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
    private final DurableEventService eventService;

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
        this(resourceRepository, titleRepository, identityRepository, auditService, transactionalOperator, null);
    }

    @Autowired
    public DefaultResourceService(ResourceRepository resourceRepository,
                                  ResourceTitleRepository titleRepository,
                                  ExternalIdentityRepository identityRepository,
                                  AuditService auditService,
                                  TransactionalOperator transactionalOperator,
                                  DurableEventService eventService) {
        this.resourceRepository = resourceRepository;
        this.titleRepository = titleRepository;
        this.identityRepository = identityRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
        this.eventService = eventService;
    }

    @Override
    public Mono<ResourceView> create(UUID ownerId, CreateResourceRequest request) {
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(
            null, ownerId, request.type(), request.title(), null, ResourceClassification.PRIVATE,
            ResourceLifecycle.ACTIVE, now, now, null, null
        );
        return transactionalOperator.transactional(resourceRepository.save(resource)
            .flatMap(saved -> titleRepository.save(new ResourceTitleEntity(
                null, saved.id(), request.locale(), request.title(), true, now, now, null
            )).then(emit("resource.resource.created", saved))
                .then(auditService.record(ownerId, "resource.create", "RESOURCE", saved.id(), "{}"))
                .then(toView(saved))));
    }

    @Override
    public Mono<ResourceView> get(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId).flatMap(this::toView);
    }

    @Override
    public Mono<ResourceView> update(UUID ownerId, UUID resourceId, UpdateResourceRequest request) {
        return transactionalOperator.transactional(owned(ownerId, resourceId).flatMap(resource -> {
            if (resource.version() == null || resource.version() != request.expectedVersion()) {
                return Mono.error(new ConflictException("resource.version-conflict", "Resource 版本已过期"));
            }
            ResourceEntity updated = new ResourceEntity(resource.id(), resource.ownerId(), resource.resourceType(),
                request.primaryTitle() == null ? resource.primaryTitle() : request.primaryTitle(),
                request.summary() == null ? resource.summary() : request.summary(), resource.dataClassification(),
                resource.lifecycle(), resource.createdAt(), Instant.now(), resource.deletedAt(), resource.version());
            return resourceRepository.save(updated)
                .flatMap(saved -> syncPrimaryTitle(resource, saved, request)
                    .then(emit("resource.resource.updated", saved))
                    .then(auditService.record(ownerId, "resource.update", "RESOURCE", resourceId, "{}"))
                    .then(toView(saved)));
        }));
    }

    private Mono<Void> syncPrimaryTitle(ResourceEntity previous, ResourceEntity updated,
                                         UpdateResourceRequest request) {
        if (request.primaryTitle() == null || request.primaryTitle().equals(previous.primaryTitle())) {
            return Mono.empty();
        }
        return titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(updated.id())
            .filter(ResourceTitleEntity::primary)
            .next()
            .flatMap(title -> titleRepository.save(new ResourceTitleEntity(title.id(), title.resourceId(),
                title.locale(), request.primaryTitle(), true, title.createdAt(), Instant.now(), title.version(),
                title.titleKind())))
            .then();
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
    public Mono<ResourceView> findByExternalIdentity(UUID ownerId, String provider, String externalType,
                                                      String externalId) {
        return identityRepository.findByProviderAndExternalTypeAndExternalId(provider, externalType, externalId)
            .switchIfEmpty(Mono.error(new NotFoundException("外部身份对应的资源不存在")))
            .flatMap(identity -> owned(ownerId, identity.resourceId()))
            .flatMap(this::toView);
    }

    @Override
    public Mono<Void> trash(UUID ownerId, UUID resourceId) {
        return trash(ownerId, resourceId, null);
    }

    @Override
    public Mono<Void> trash(UUID ownerId, UUID resourceId, long expectedVersion) {
        return trash(ownerId, resourceId, Long.valueOf(expectedVersion));
    }

    private Mono<Void> trash(UUID ownerId, UUID resourceId, Long expectedVersion) {
        return owned(ownerId, resourceId)
            .flatMap(resource -> {
                checkVersion(resource.version(), expectedVersion);
                if (resource.lifecycle() == ResourceLifecycle.TRASHED) {
                    return Mono.empty();
                }
                ResourceEntity trashed = new ResourceEntity(
                    resource.id(), resource.ownerId(), resource.resourceType(), resource.primaryTitle(), resource.summary(),
                    resource.dataClassification(), ResourceLifecycle.TRASHED,
                    resource.createdAt(), Instant.now(), Instant.now(), resource.version()
                );
                return resourceRepository.save(trashed)
                    .then(emit("resource.resource.trashed", trashed))
                    .then(auditService.record(ownerId, "resource.trash", "RESOURCE", resourceId, "{}"));
            });
    }

    @Override
    public Mono<ResourceView> archive(UUID ownerId, UUID resourceId) {
        return archive(ownerId, resourceId, null);
    }

    @Override
    public Mono<ResourceView> archive(UUID ownerId, UUID resourceId, long expectedVersion) {
        return archive(ownerId, resourceId, Long.valueOf(expectedVersion));
    }

    private Mono<ResourceView> archive(UUID ownerId, UUID resourceId, Long expectedVersion) {
        return owned(ownerId, resourceId)
            .flatMap(resource -> {
                checkVersion(resource.version(), expectedVersion);
                if (resource.lifecycle() == ResourceLifecycle.ARCHIVED) {
                    return toView(resource);
                }
                if (resource.lifecycle() != ResourceLifecycle.ACTIVE) {
                    return Mono.error(new ConflictException("只有活动 Resource 才能归档"));
                }
                ResourceEntity archived = new ResourceEntity(
                    resource.id(), resource.ownerId(), resource.resourceType(), resource.primaryTitle(), resource.summary(),
                    resource.dataClassification(), ResourceLifecycle.ARCHIVED,
                    resource.createdAt(), Instant.now(), resource.deletedAt(), resource.version()
                );
                return resourceRepository.save(archived)
                    .flatMap(saved -> emit("resource.resource.archived", saved)
                        .then(auditService.record(ownerId, "resource.archive", "RESOURCE", resourceId, "{}"))
                        .then(toView(saved)));
            })
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<ResourceView> restore(UUID ownerId, UUID resourceId) {
        return restore(ownerId, resourceId, null);
    }

    @Override
    public Mono<ResourceView> restore(UUID ownerId, UUID resourceId, long expectedVersion) {
        return restore(ownerId, resourceId, Long.valueOf(expectedVersion));
    }

    private Mono<ResourceView> restore(UUID ownerId, UUID resourceId, Long expectedVersion) {
        return owned(ownerId, resourceId)
            .flatMap(resource -> {
                checkVersion(resource.version(), expectedVersion);
                if (resource.lifecycle() != ResourceLifecycle.TRASHED
                    && resource.lifecycle() != ResourceLifecycle.ARCHIVED) {
                    return Mono.error(new ConflictException("只有已归档或已移入回收站的 Resource 才能恢复"));
                }
                ResourceEntity restored = new ResourceEntity(
                    resource.id(), resource.ownerId(), resource.resourceType(), resource.primaryTitle(), resource.summary(),
                    resource.dataClassification(), ResourceLifecycle.ACTIVE,
                    resource.createdAt(), Instant.now(), null, resource.version()
                );
                return resourceRepository.save(restored)
                    .flatMap(saved -> emit("resource.resource.restored", saved)
                        .then(auditService.record(ownerId, "resource.restore", "RESOURCE", resourceId, "{}"))
                        .then(toView(saved)));
            });
    }

    private void checkVersion(Long actualVersion, Long expectedVersion) {
        if (expectedVersion != null && (actualVersion == null ? 0 : actualVersion) != expectedVersion) {
            throw new PreconditionFailedException("If-Match 与 Resource 当前版本不匹配");
        }
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

    @Override
    public Mono<Void> detachExternalIdentity(UUID ownerId, UUID resourceId, UUID identityId) {
        return transactionalOperator.transactional(owned(ownerId, resourceId)
            .then(identityRepository.findByIdAndResourceId(identityId, resourceId)
                .switchIfEmpty(Mono.error(new NotFoundException("外部身份不存在或无权访问")))
                .flatMap(identity -> identityRepository.deleteById(identity.id())
                    .then(auditService.record(ownerId, "resource.external-identity.delete",
                        "RESOURCE", resourceId, "{}"))))
            .then());
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }

    private Mono<Void> emit(String eventType, ResourceEntity resource) {
        if (eventService == null) {
            return Mono.empty();
        }
        String payload = "{\"resource_id\":\"" + resource.id() + "\",\"lifecycle\":\""
            + resource.lifecycle() + "\",\"version\":" + resource.version() + "}";
        return eventService.append(eventType, 1, "resource", resource.id(), payload).then();
    }

    private Mono<ResourceView> toView(ResourceEntity resource) {
        Mono<List<ResourceTitleView>> titles = titleRepository
            .findAllByResourceIdOrderByPrimaryDescLocaleAsc(resource.id())
            .map(title -> new ResourceTitleView(title.id(), title.locale(), title.title(), title.primary(), title.titleKind()))
            .collectList();
        Mono<List<ExternalIdentityView>> identities = identityRepository
            .findAllByResourceIdOrderByProviderAsc(resource.id())
            .map(this::toIdentityView)
            .collectList();
        return Mono.zip(titles, identities)
            .map(parts -> new ResourceView(resource.id(), resource.resourceType(), resource.primaryTitle(),
                resource.summary(), resource.dataClassification(), resource.lifecycle(), parts.getT1(), parts.getT2(),
                resource.createdAt(), resource.updatedAt(), resource.version()));
    }

    private ExternalIdentityView toIdentityView(ExternalIdentityEntity identity) {
        return new ExternalIdentityView(identity.id(), identity.provider(), identity.externalType(),
            identity.externalId());
    }
}
