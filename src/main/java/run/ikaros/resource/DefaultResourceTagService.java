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
import run.ikaros.event.DurableEventService;

/**
 * 默认用户标签服务实现，标签权限始终绑定当前用户和 Resource。
 */
@Service
public class DefaultResourceTagService implements ResourceTagService {
    private final ResourceRepository resourceRepository;
    private final ResourceTagRepository tagRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;
    private final DurableEventService eventService;

    /** 创建用户标签服务。 */
    public DefaultResourceTagService(ResourceRepository resourceRepository, ResourceTagRepository tagRepository,
                                     AuditService auditService, TransactionalOperator transactionalOperator) {
        this(resourceRepository, tagRepository, auditService, transactionalOperator, null);
    }

    public DefaultResourceTagService(ResourceRepository resourceRepository, ResourceTagRepository tagRepository,
                                     AuditService auditService, TransactionalOperator transactionalOperator,
                                     DurableEventService eventService) {
        this.resourceRepository = resourceRepository;
        this.tagRepository = tagRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
        this.eventService = eventService;
    }

    @Override
    public Mono<ResourceTagView> add(UUID ownerId, UUID resourceId, CreateResourceTagRequest request) {
        return owned(ownerId, resourceId)
            .then(tagRepository.findByOwnerIdAndResourceIdAndName(ownerId, resourceId, request.name().trim())
                .map(this::toView)
                .switchIfEmpty(Mono.defer(() -> tagRepository.save(new ResourceTagEntity(null, ownerId, resourceId,
                    request.name().trim(), request.color(), Instant.now(), Instant.now(), null))
                    .onErrorMap(DuplicateKeyException.class, ex -> new ConflictException("资源标签已存在"))
                    .flatMap(saved -> emitCreated(saved)
                        .then(emitAdded(saved))
                        .then(auditService.record(ownerId, "resource.tag.add", "RESOURCE", resourceId, "{}"))
                        .thenReturn(toView(saved))))))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<List<ResourceTagView>> list(UUID ownerId, UUID resourceId) {
        return owned(ownerId, resourceId)
            .thenMany(tagRepository.findAllByOwnerIdAndResourceIdOrderByNameAsc(ownerId, resourceId))
            .map(this::toView)
            .collectList();
    }

    @Override
    public Mono<PageResponse<ResourceTagView>> listCatalog(UUID ownerId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        return tagRepository.findAllByOwnerIdOrderByNameAsc(ownerId)
            .map(this::toView)
            .collectList()
            .map(all -> {
                List<ResourceTagView> unique = all.stream()
                    .collect(java.util.stream.Collectors.toMap(ResourceTagView::name, value -> value,
                        (first, ignored) -> first, java.util.LinkedHashMap::new))
                    .values().stream().toList();
                return new PageResponse<>(unique.stream().skip((long) page * size).limit(size).toList(),
                    unique.size(), page, size);
            });
    }

    @Override
    public Mono<Void> remove(UUID ownerId, UUID resourceId, UUID tagId) {
        return owned(ownerId, resourceId)
            .then(tagRepository.findByIdAndOwnerIdAndResourceId(tagId, ownerId, resourceId)
                .switchIfEmpty(Mono.error(new NotFoundException("资源标签不存在或无权访问")))
                .flatMap(tag -> tagRepository.deleteById(tag.id())
                    .then(emitRemoved(tag))
                    .then(auditService.record(ownerId, "resource.tag.remove", "RESOURCE", resourceId, "{}"))))
            .then()
            .as(transactionalOperator::transactional);
    }

    private ResourceTagView toView(ResourceTagEntity tag) {
        return new ResourceTagView(tag.id(), tag.name(), tag.color());
    }

    private Mono<Void> emitCreated(ResourceTagEntity tag) {
        if (eventService == null) return Mono.empty();
        return eventService.append("resource.tag.created", 1, "resource_tag", tag.id(),
            "{\"tag_id\":\"" + tag.id() + "\",\"scope_key\":\"user\"}").then();
    }

    private Mono<Void> emitAdded(ResourceTagEntity tag) {
        if (eventService == null) return Mono.empty();
        return eventService.append("resource.tag.added", 1, "resource", tag.resourceId(),
            "{\"resource_id\":\"" + tag.resourceId() + "\",\"tag_id\":\"" + tag.id() + "\"}").then();
    }

    private Mono<Void> emitRemoved(ResourceTagEntity tag) {
        if (eventService == null) return Mono.empty();
        return eventService.append("resource.tag.removed", 1, "resource", tag.resourceId(),
            "{\"resource_id\":\"" + tag.resourceId() + "\",\"tag_id\":\"" + tag.id() + "\"}").then();
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }
}
