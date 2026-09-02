package run.ikaros.identity;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PageResponse;

/**
 * 默认用户服务，维护用户状态、角色绑定与对应审计记录。
 */
@Service
public class DefaultUserService implements UserService {
    private final PlatformUserRepository userRepository;
    private final PlatformRoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditService auditService;

    /**
     * 创建用户服务。
     *
     * @param userRepository 用户仓储
     * @param roleRepository 角色仓储
     * @param userRoleRepository 用户角色绑定仓储
     * @param auditService 审计服务
     */
    public DefaultUserService(PlatformUserRepository userRepository, PlatformRoleRepository roleRepository,
                              UserRoleRepository userRoleRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditService = auditService;
    }

    @Override
    public Mono<UserView> create(UUID actorId, CreateUserRequest request) {
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(null, request.username().trim(), request.displayName().trim(),
            normalizeEmail(request.email()), UserStatus.PENDING, now, now, null, null);
        return userRepository.save(user)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("用户名或邮箱已存在"))
            .flatMap(saved -> auditService.record(actorId, "identity.user.create", "USER", saved.id(), "{}")
                .then(toView(saved)));
    }

    @Override
    public Mono<UserView> get(UUID userId) {
        return requiredUser(userId).flatMap(this::toView);
    }

    @Override
    public Mono<PageResponse<UserView>> list(UserStatus status, String query, int page, int size) {
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
                status, user.createdAt(), Instant.now(), user.lastLoginAt(), user.version());
            return userRepository.save(changed)
                .flatMap(saved -> auditService.record(actorId, "identity.user.status.change", "USER", userId, "{}")
                    .then(toView(saved)));
        });
    }

    @Override
    public Mono<Void> assignRole(UUID actorId, UUID userId, UUID roleId) {
        Instant now = Instant.now();
        return Mono.zip(requiredUser(userId), requiredRole(roleId))
            .flatMap(ignored -> userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .hasElement()
                .flatMap(exists -> exists ? Mono.<Void>empty() : userRoleRepository.save(new UserRoleEntity(
                    null, userId, roleId, now, null
                )).then()))
            .then(auditService.record(actorId, "identity.user.role.assign", "USER", userId, "{}"));
    }

    private Mono<PlatformUserEntity> requiredUser(UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在")));
    }

    private Mono<PlatformRoleEntity> requiredRole(UUID roleId) {
        return roleRepository.findById(roleId)
            .switchIfEmpty(Mono.error(new NotFoundException("角色不存在")));
    }

    private Mono<UserView> toView(PlatformUserEntity user) {
        return userRoleRepository.findAllByUserId(user.id())
            .flatMap(binding -> roleRepository.findById(binding.roleId()))
            .map(PlatformRoleEntity::code)
            .sort()
            .collectList()
            .map(roles -> new UserView(user.id(), user.username(), user.displayName(), user.email(), user.status(), roles,
                user.createdAt(), user.lastLoginAt()));
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
