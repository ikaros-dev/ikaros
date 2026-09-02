package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class DefaultUserResourceStateService implements UserResourceStateService {
    private final ResourceRepository resources;
    private final UserResourceStateRepository states;
    private final TransactionalOperator transaction;

    public DefaultUserResourceStateService(ResourceRepository resources, UserResourceStateRepository states,
                                           TransactionalOperator transaction) {
        this.resources = resources; this.states = states; this.transaction = transaction;
    }

    @Override
    public Mono<UserResourceStateView> get(UUID userId, UUID resourceId) {
        return owned(userId, resourceId).then(states.findByUserIdAndResourceId(userId, resourceId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户资源状态不存在"))))
            .map(this::view);
    }

    @Override
    public Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request) {
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
            .flatMap(current -> states.save(new UserResourceStateEntity(userId, resourceId, request.favorite(),
                request.rating(), request.statusCode(), request.progressValue(), request.progressUnit(),
                request.progressValue() == null ? current.lastAccessedAt() : Instant.now(), current.version(), Instant.now())))
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
}
