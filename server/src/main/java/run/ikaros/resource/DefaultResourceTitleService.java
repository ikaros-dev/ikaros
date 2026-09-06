package run.ikaros.resource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

/**
 * 默认 Resource 标题服务，维护语言唯一性与主标题不变式。
 */
@Service
public class DefaultResourceTitleService implements ResourceTitleService {
    private final ResourceRepository resourceRepository;
    private final ResourceTitleRepository titleRepository;
    private final AuditService auditService;
    private final TransactionalOperator transactionalOperator;

    /**
     * 创建 Resource 标题服务。
     *
     * @param resourceRepository Resource 仓储
     * @param titleRepository 标题仓储
     * @param auditService 审计服务
     * @param transactionalOperator 响应式事务操作器
     */
    public DefaultResourceTitleService(ResourceRepository resourceRepository,
                                       ResourceTitleRepository titleRepository,
                                       AuditService auditService,
                                       TransactionalOperator transactionalOperator) {
        this.resourceRepository = resourceRepository;
        this.titleRepository = titleRepository;
        this.auditService = auditService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<ResourceTitleView> set(UUID ownerId, UUID resourceId, SetResourceTitleRequest request) {
        return owned(ownerId, resourceId)
            .then(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId).collectList())
            .flatMap(existing -> saveTitle(ownerId, resourceId, existing, request))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> delete(UUID ownerId, UUID resourceId, UUID titleId) {
        return owned(ownerId, resourceId)
            .then(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId).collectList())
            .flatMap(existing -> {
                ResourceTitleEntity target = existing.stream()
                    .filter(title -> title.id().equals(titleId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("标题不存在或无权访问"));
                if (existing.size() == 1) {
                    return Mono.error(new ConflictException("Resource 至少需要保留一个标题"));
                }
                Mono<Void> promote = target.primary()
                    ? titleRepository.save(withPrimary(existing.stream()
                        .filter(title -> !title.id().equals(titleId)).findFirst().orElseThrow(), true)).then()
                    : Mono.empty();
                return promote.then(titleRepository.deleteById(titleId))
                    .then(auditService.record(ownerId, "resource.title.delete", "RESOURCE", resourceId, "{}"));
            })
            .as(transactionalOperator::transactional);
    }

    private Mono<ResourceTitleView> saveTitle(UUID ownerId, UUID resourceId, List<ResourceTitleEntity> existing,
                                              SetResourceTitleRequest request) {
        ResourceTitleKind kind = request.kind() == null ? ResourceTitleKind.TITLE : request.kind();
        if (kind == ResourceTitleKind.ALIAS && request.primary()) {
            return Mono.error(new ConflictException("Alias 不能作为 Resource 主标题"));
        }
        Instant now = Instant.now();
        ResourceTitleEntity current = existing.stream()
            .filter(title -> title.locale().equalsIgnoreCase(request.locale()))
            .findFirst()
            .orElse(null);
        boolean primary = request.primary() || current == null && existing.isEmpty()
            || current != null && current.primary() && !request.primary();
        ResourceTitleEntity target = current == null
            ? new ResourceTitleEntity(null, resourceId, request.locale(), request.title(), primary, now, now, null,
                kind)
            : new ResourceTitleEntity(current.id(), resourceId, current.locale(), request.title(), primary,
                current.createdAt(), now, current.version(),
                request.kind() == null ? current.titleKind() : kind);
        List<ResourceTitleEntity> updated = new ArrayList<>();
        for (ResourceTitleEntity title : existing) {
            updated.add(request.primary() && !title.id().equals(current == null ? null : current.id())
                ? withPrimary(title, false) : title.id().equals(target.id()) ? target : title);
        }
        if (current == null) {
            updated.add(target);
        }
        return titleRepository.saveAll(updated).collectList()
            .flatMap(saved -> saved.stream().filter(title -> title.locale().equalsIgnoreCase(target.locale())).findFirst()
                .map(value -> auditService.record(ownerId, "resource.title.set", "RESOURCE", resourceId, "{}")
                    .thenReturn(toView(value)))
                .orElseGet(() -> Mono.error(new ConflictException("标题保存失败"))));
    }

    private ResourceTitleEntity withPrimary(ResourceTitleEntity title, boolean primary) {
        return new ResourceTitleEntity(title.id(), title.resourceId(), title.locale(), title.title(), primary,
            title.createdAt(), Instant.now(), title.version(), title.titleKind());
    }

    private ResourceTitleView toView(ResourceTitleEntity title) {
        return new ResourceTitleView(title.id(), title.locale(), title.title(), title.primary(), title.titleKind());
    }

    private Mono<ResourceEntity> owned(UUID ownerId, UUID resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")));
    }
}
