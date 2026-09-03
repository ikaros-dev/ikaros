package run.ikaros.resource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.event.DurableEventService;

@Service
public class DefaultUserResourceStateService implements UserResourceStateService {
    private final ResourceRepository resources;
    private final UserResourceStateRepository states;
    private final TransactionalOperator transaction;
    private final DurableEventService eventService;

    public DefaultUserResourceStateService(ResourceRepository resources, UserResourceStateRepository states,
                                           TransactionalOperator transaction) {
        this(resources, states, transaction, null);
    }

    @Autowired
    public DefaultUserResourceStateService(ResourceRepository resources, UserResourceStateRepository states,
                                           TransactionalOperator transaction, DurableEventService eventService) {
        this.resources = resources;
        this.states = states;
        this.transaction = transaction;
        this.eventService = eventService;
    }

    @Override
    public Mono<UserResourceStateView> get(UUID userId, UUID resourceId) {
        return owned(userId, resourceId).then(states.findByUserIdAndResourceId(userId, resourceId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户资源状态不存在"))))
            .map(this::view);
    }

    @Override
    public Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request) {
        return setInternal(userId, resourceId, request, null);
    }

    @Override
    public Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request,
                                           long expectedVersion) {
        return setInternal(userId, resourceId, request, expectedVersion);
    }

    private Mono<UserResourceStateView> setInternal(UUID userId, UUID resourceId,
                                                     UserResourceStateRequest request, Long expectedVersion) {
        if (request.rating() != null && (request.rating().signum() < 0 || request.rating().doubleValue() > 10)) {
            return Mono.error(new IllegalArgumentException("评分必须介于 0 和 10 之间"));
        }
        if (request.progressValue() != null && request.progressValue().signum() < 0) {
            return Mono.error(new IllegalArgumentException("进度不能为负数"));
        }
        return owned(userId, resourceId)
            .then(states.findByUserIdAndResourceId(userId, resourceId))
            .defaultIfEmpty(new UserResourceStateEntity(userId, resourceId, false, null, null, null, null,
                null, null, null))
            .flatMap(current -> {
                long actualVersion = current.version() == null ? 0 : current.version();
                if (expectedVersion != null && actualVersion != expectedVersion) {
                    return Mono.error(new PreconditionFailedException("If-Match 与用户资源状态当前版本不匹配"));
                }
                UserResourceStateEntity next = new UserResourceStateEntity(userId, resourceId, request.favorite(),
                    request.rating(), request.statusCode(), request.progressValue(), request.progressUnit(),
                    request.progressValue() == null ? current.lastAccessedAt() : Instant.now(), current.version(), Instant.now());
                return states.save(next)
                    .flatMap(saved -> emitChanged(current, saved).thenReturn(saved));
            })
            .map(this::view).as(transaction::transactional);
    }

    private Mono<Void> owned(UUID userId, UUID resourceId) {
        return resources.findByIdAndOwnerId(resourceId, userId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问"))).then();
    }

    private UserResourceStateView view(UserResourceStateEntity state) {
        return new UserResourceStateView(state.userId(), state.resourceId(), state.favorite(), state.rating(),
            state.statusCode(), state.progressValue(), state.progressUnit(), state.lastAccessedAt(),
            state.version(), state.updatedAt());
    }

    private Mono<Void> emitChanged(UserResourceStateEntity previous, UserResourceStateEntity current) {
        if (eventService == null) {
            return Mono.empty();
        }
        List<String> changed = new ArrayList<>();
        if (previous.favorite() != current.favorite()) changed.add("favorite");
        if (!Objects.equals(previous.rating(), current.rating())) changed.add("rating");
        if (!Objects.equals(previous.statusCode(), current.statusCode())) changed.add("status_code");
        if (!Objects.equals(previous.progressValue(), current.progressValue())) changed.add("progress_value");
        if (!Objects.equals(previous.progressUnit(), current.progressUnit())) changed.add("progress_unit");
        if (!Objects.equals(previous.lastAccessedAt(), current.lastAccessedAt())) changed.add("last_accessed_at");
        String fields = changed.stream().map(this::quoteJson).collect(java.util.stream.Collectors.joining(","));
        String payload = "{\"user_id\":\"" + current.userId() + "\",\"resource_id\":\""
            + current.resourceId() + "\",\"changed_fields\":[" + fields + "],\"version\":"
            + (current.version() == null ? 0 : current.version()) + "}";
        return eventService.append("resource.user-state.changed", 1, "resource", current.resourceId(), payload).then();
    }

    private String quoteJson(String value) {
        return "\"" + value + "\"";
    }
}
