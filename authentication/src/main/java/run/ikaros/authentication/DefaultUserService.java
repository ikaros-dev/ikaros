package run.ikaros.authentication;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.common.ConflictException;
import run.ikaros.common.ForbiddenException;
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
    private final PasswordCredentialRepository credentialRepository;
    private final RoleMembershipQuery roleMembershipQuery;
    private final AuditService auditService;
    private final DurableEventPublisher eventService;
    private final TransactionalOperator transaction;

    /**
     * 创建用户服务。
     *
     * @param userRepository 用户仓储
     * @param roleMembershipQuery 角色成员查询能力
     * @param auditService 审计服务
     */
    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService) {
        this(userRepository, roleMembershipQuery, auditService, null, null, null);
    }

    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService,
                              DurableEventPublisher eventService) {
        this(userRepository, roleMembershipQuery, auditService, eventService, null, null);
    }

    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService, DurableEventPublisher eventService,
                              TransactionalOperator transaction) {
        this(userRepository, roleMembershipQuery, auditService, eventService, transaction, null);
    }

    @Autowired
    public DefaultUserService(PlatformUserRepository userRepository, RoleMembershipQuery roleMembershipQuery,
                              AuditService auditService, DurableEventPublisher eventService,
                              TransactionalOperator transaction, PasswordCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleMembershipQuery = roleMembershipQuery;
        this.auditService = auditService;
        this.eventService = eventService;
        this.transaction = transaction;
    }

    @Override
    public Mono<UserView> create(UUID actorId, CreateUserRequest request) {
        String username = request.username().trim();
        Instant now = Instant.now();
        return userRepository.findIncludingDeletedByUsername(username)
            .flatMap(existing -> existing.isDel() == 1
                ? restore(existing, request, actorId, now)
                : Mono.error(new ConflictException("用户名已存在")))
            .switchIfEmpty(createNew(actorId, request, username, now));
    }

    private Mono<UserView> createNew(UUID actorId, CreateUserRequest request, String username, Instant now) {
        PlatformUserEntity user = new PlatformUserEntity(null, username, request.displayName().trim(),
            normalizeEmail(request.email()), UserStatus.ACTIVE, now, now, null, 0L, null);
        Mono<UserView> operation = userRepository.save(user)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("用户名或邮箱已存在"))
            .flatMap(saved -> credentialRepository.save(new PasswordCredentialEntity(null, saved.id(),
                    PasswordHashService.hash(request.password()), now, now, null))
                .then(emitUserCreated(saved))
                .then(adminAudit(actorId, "identity.user.create", saved.id(), "{}"))
                .then(toView(saved)));
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    @Override
    public Mono<UserView> get(UUID userId) {
        return requiredUser(userId).flatMap(this::toView);
    }

    @Override
    public Mono<UserView> update(UUID actorId, UUID userId, UpdateUserRequest request) {
        if (actorId.equals(userId)) {
            return Mono.error(new ForbiddenException("不允许修改当前登录用户"));
        }
        Mono<UserView> operation = requiredUser(userId).flatMap(user -> {
            UserStatus status = request.status();
            PlatformUserEntity updated = new PlatformUserEntity(user.id(), request.username().trim(),
                request.displayName().trim(), normalizeEmail(request.email()), status, user.createdAt(), Instant.now(),
                user.lastLoginAt(), status == user.status() ? user.securityVersion() : user.securityVersion() + 1,
                user.version(), user.isDel());
            return userRepository.save(updated)
                .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("用户名或邮箱已存在"))
                .flatMap(saved -> emitStatusChanged(saved)
                    .then(adminAudit(actorId, "identity.user.update", userId, "{}"))
                    .then(toView(saved)));
        });
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    @Override
    public Mono<PageResponse<UserView>> list(UserStatus status, String query, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        String keyword = query == null ? "" : query.trim();
        return userRepository.findAll()
            .filter(user -> user.isDel() == 0)
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
        if (actorId.equals(userId)) {
            return Mono.error(new ForbiddenException("不允许修改当前登录用户"));
        }
        Mono<UserView> operation = requiredUser(userId).flatMap(user -> {
            PlatformUserEntity changed = new PlatformUserEntity(user.id(), user.username(), user.displayName(), user.email(),
                status, user.createdAt(), Instant.now(), user.lastLoginAt(),
                status == user.status() ? user.securityVersion() : user.securityVersion() + 1, user.version());
            return userRepository.save(changed)
                .flatMap(saved -> emitStatusChanged(saved)
                    .then(adminAudit(actorId, "identity.user.status.change", userId, "{}"))
                    .then(toView(saved)));
        });
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    @Override
    public Mono<Void> delete(UUID actorId, UUID userId) {
        if (actorId.equals(userId)) {
            return Mono.error(new ForbiddenException("不允许删除当前登录用户"));
        }
        Mono<Void> operation = requiredUser(userId).flatMap(user -> {
            if (user.status() == UserStatus.DEACTIVATED) return Mono.empty();
            PlatformUserEntity deleted = new PlatformUserEntity(user.id(), user.username(), user.displayName(), user.email(),
                UserStatus.DEACTIVATED, user.createdAt(), Instant.now(), user.lastLoginAt(),
                user.securityVersion() + 1, user.version(), 1);
            return userRepository.save(deleted)
                .flatMap(saved -> emitUserDeactivated(saved)
                    .then(adminAudit(actorId, "identity.user.delete", userId, "{}")));
        });
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    private Mono<UserView> restore(PlatformUserEntity existing, CreateUserRequest request, UUID actorId, Instant now) {
        PlatformUserEntity restored = new PlatformUserEntity(existing.id(), existing.username(),
            request.displayName().trim(), normalizeEmail(request.email()), UserStatus.ACTIVE,
            existing.createdAt(), now, existing.lastLoginAt(), existing.securityVersion() + 1,
            existing.version(), 0);
        Mono<UserView> operation = userRepository.save(restored)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("用户名或邮箱已存在"))
            .flatMap(saved -> replacePassword(saved.id(), request.password(), now)
                .then(emitUserCreated(saved))
                .then(adminAudit(actorId, "identity.user.restore", saved.id(), "{}"))
                .then(toView(saved)));
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    private Mono<Void> replacePassword(UUID userId, String password, Instant now) {
        return credentialRepository.findByUserId(userId)
            .flatMap(existing -> credentialRepository.save(new PasswordCredentialEntity(existing.id(), userId,
                PasswordHashService.hash(password), existing.createdAt(), now, existing.version())))
            .switchIfEmpty(Mono.defer(() -> credentialRepository.save(new PasswordCredentialEntity(null, userId,
                PasswordHashService.hash(password), now, now, null))))
            .then();
    }

    @Override
    public Mono<TokenInvalidationView> invalidateTokens(UUID actorId, UUID userId) {
        Mono<TokenInvalidationView> operation = requiredUser(userId).flatMap(user -> {
            PlatformUserEntity invalidated = new PlatformUserEntity(user.id(), user.username(), user.displayName(),
                user.email(), user.status(), user.createdAt(), Instant.now(), user.lastLoginAt(),
                user.securityVersion() + 1, user.version());
            return userRepository.save(invalidated).flatMap(saved ->
                emitTokensInvalidated(saved)
                    .then(recordTokenInvalidation(actorId, saved))
                    .thenReturn(new TokenInvalidationView(saved.id(), saved.securityVersion())));
        });
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    private Mono<Void> recordTokenInvalidation(UUID actorId, PlatformUserEntity user) {
        String details = "{\"security_version\":" + user.securityVersion() + "}";
        if (!actorId.equals(user.id())) {
            return adminAudit(actorId, "identity.user.tokens.invalidate", user.id(), details);
        }
        return auditService.record(new AuditEventCommand(AuditActorType.USER, actorId,
            "identity.user.tokens.invalidate", "USER", user.id(), AuditResult.SUCCESS, AuditRiskLevel.SENSITIVE,
            details, 1, null));
    }

    private Mono<Void> adminAudit(UUID actorId, String action, UUID targetId, String details) {
        return auditService.record(new AuditEventCommand(AuditActorType.ADMIN, actorId, action, "USER", targetId,
            AuditResult.SUCCESS, AuditRiskLevel.HIGH, details, 1, null));
    }

    private Mono<Void> emitTokensInvalidated(PlatformUserEntity user) {
        if (eventService == null) return Mono.empty();
        return eventService.append(new EventAppendRequest("authentication.user.tokens-invalidated", 1,
            "authentication", "user", user.id(),
            "{\"user_id\":\"" + user.id() + "\",\"security_version\":"
                + user.securityVersion() + "}")).then();
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

    private Mono<Void> emitUserDeactivated(PlatformUserEntity user) {
        if (eventService == null) return Mono.empty();
        return eventService.append(new EventAppendRequest("authentication.user.deactivated", 1,
            "authentication", "user", user.id(),
            "{\"user_id\":\"" + user.id() + "\",\"security_version\":"
                + user.securityVersion() + "}")).then();
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
