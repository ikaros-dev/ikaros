package run.ikaros.authentication;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.authorization.api.RoleMembershipQuery;

/**
 * 默认用户服务，维护用户身份状态与对应审计记录。
 */
@Service
public class DefaultUserService implements UserService {
    private static final int MAX_PAGE_SIZE = 100;
    private final PlatformUserRepository userRepository;
    private final RoleMembershipQuery roleMembershipQuery;
    private final AuditService auditService;
    private final DurableEventPublisher eventService;

    /**
     * 创建用户服务。
     *
     * @param userRepository 用户仓储
     * @param roleMembershipQuery 角色成员查询能力
     * @param auditService 审计服务
     */
    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService) {
        this(userRepository, roleMembershipQuery, auditService, null);
    }

    @Autowired
    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService,
                              DurableEventPublisher eventService) {
        this.userRepository = userRepository;
        this.roleMembershipQuery = roleMembershipQuery;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Override
    public Mono<UserView> create(UUID actorId, CreateUserRequest request) {
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(null, request.username().trim(), request.displayName().trim(),
            normalizeEmail(request.email()), UserStatus.PENDING, now, now, null, 0L, null);
        return userRepository.save(user)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("用户名或邮箱已存在"))
            .flatMap(saved -> emitUserCreated(saved)
                .then(auditService.record(actorId, "identity.user.create", "USER", saved.id(), "{}"))
                .then(toView(saved)));
    }

    @Override
    public Mono<UserView> get(UUID userId) {
        return requiredUser(userId).flatMap(this::toView);
    }

    @Override
    public Mono<PageResponse<UserView>> list(UserStatus status, String query, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        String keyword = query == null ? "" : query.trim();
        return userRepository.findAll()
            .filter(user -> status == null || user.status() == status)
            .filter(user -> keyword.isEmpty() || user.username().toLowerCase().contains(keyword.toLowerCase()))
            .sort(Comparator.comparing(PlatformUserEntity::createdAt).reversed())
            .collectList()
            .flatMap(users -> Mono.zip(
                reactor.core.publisher.Flux.fromIterable(users).skip((long) page * size).take(size)
                    .flatMap(this::toView).collectList(),
                Mono.just((long) users.size())
            ).map(parts -> new PageResponse<>(parts.getT1(), parts.getT2(), page, size)));
    }

    @Override
    public Mono<UserView> changeStatus(UUID actorId, UUID userId, UserStatus status) {
        return requiredUser(userId).flatMap(user -> {
            PlatformUserEntity changed = new PlatformUserEntity(user.id(), user.username(), user.displayName(), user.email(),
                status, user.createdAt(), Instant.now(), user.lastLoginAt(),
                status == user.status() ? user.securityVersion() : user.securityVersion() + 1, user.version());
            return userRepository.save(changed)
                .flatMap(saved -> emitStatusChanged(saved)
                    .then(auditService.record(actorId, "identity.user.status.change", "USER", userId, "{}"))
                    .then(toView(saved)));
        });
    }

    private Mono<Void> emitStatusChanged(PlatformUserEntity user) {
        if (eventService == null) return Mono.empty();
        String eventType = user.status() == UserStatus.DISABLED ? "authentication.user.disabled"
            : user.status() == UserStatus.ACTIVE ? "authentication.user.enabled" : null;
        if (eventType == null) return Mono.empty();
        return eventService.append(new EventAppendRequest(eventType, 1, "authentication", "user", user.id(),
            "{\"user_id\":\"" + user.id() + "\",\"security_version\":"
                + user.securityVersion() + "}")).then();
    }

    private Mono<Void> emitUserCreated(PlatformUserEntity user) {
        if (eventService == null) return Mono.empty();
        return eventService.append(new EventAppendRequest("authentication.user.created", 1, "authentication", "user", user.id(),
            "{\"user_id\":\"" + user.id() + "\"}")).then();
    }


    private Mono<PlatformUserEntity> requiredUser(UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在")));
    }

    private Mono<UserView> toView(PlatformUserEntity user) {
        return roleMembershipQuery.roleCodesFor(user.id())
            .map(roles -> new UserView(user.id(), user.username(), user.displayName(), user.email(), user.status(), roles,
                user.createdAt(), user.lastLoginAt()));
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
