package run.ikaros.authorization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.authorization.api.PlatformPermission;
import run.ikaros.common.ConflictException;
import run.ikaros.common.ForbiddenException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;

/**
 * 默认角色服务，确保权限只能由平台注册表中声明的能力构成。
 */
@Service
public class DefaultRoleService implements RoleService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final PlatformRoleRepository roleRepository;
    private final RolePermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditService auditService;
    private final DurableEventPublisher eventService;
    private final TransactionalOperator transaction;

    /**
     * 创建角色服务。
     *
     * @param roleRepository 角色仓储
     * @param permissionRepository 角色权限绑定仓储
     * @param auditService 审计服务
     */
    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService) {
        this(roleRepository, permissionRepository, auditService, null, null);
    }

    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService, DurableEventPublisher eventService) {
        this(roleRepository, permissionRepository, auditService, eventService, null, null);
    }

    @Autowired
    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService, DurableEventPublisher eventService,
                              UserRoleRepository userRoleRepository) {
        this(roleRepository, permissionRepository, auditService, eventService, userRoleRepository, null);
    }

    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService, DurableEventPublisher eventService,
                              UserRoleRepository userRoleRepository, TransactionalOperator transaction) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.userRoleRepository = userRoleRepository;
        this.transaction = transaction;
    }

    @Override
    public Mono<RoleView> create(UUID actorId, CreateRoleRequest request) {
        if (request == null || request.code() == null || request.name() == null
            || !request.code().matches("[A-Z][A-Z0-9_]*") || request.code().length() > 96
            || request.name().isBlank() || request.name().length() > 128
            || request.description() != null && request.description().length() > 2000) {
            return Mono.error(new IllegalArgumentException("角色资料不合法"));
        }
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(null, request.code().trim(), request.name().trim(),
            request.description(), false, now, now, null);
        Mono<RoleView> operation = roleRepository.save(role)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("角色编码已存在"))
            .flatMap(saved -> emit("authorization.role.created", saved.id(),
                    "{\"role_id\":\"" + saved.id() + "\",\"role_key\":\"" + saved.code() + "\"}")
                .then(adminAudit(actorId, "identity.role.create", "ROLE", saved.id()))
                .then(toView(saved)));
        return transactional(operation);
    }

    @Override
    public Mono<RoleView> update(UUID actorId, UUID roleId, UpdateRoleRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() || request.name().length() > 128
            || request.description() != null && request.description().length() > 2000) {
            return Mono.error(new IllegalArgumentException("角色资料不合法"));
        }
        Mono<RoleView> operation = requiredCustomRole(roleId).flatMap(role -> {
            PlatformRoleEntity updated = new PlatformRoleEntity(role.id(), role.code(), request.name().trim(),
                request.description(), false, role.createdAt(), Instant.now(), role.version());
            return roleRepository.save(updated)
                .flatMap(saved -> emit("authorization.role.updated", saved.id(), rolePayload(saved))
                    .then(adminAudit(actorId, "identity.role.update", "ROLE", saved.id()))
                    .then(toView(saved)));
        });
        return transactional(operation);
    }

    @Override
    public Mono<Void> delete(UUID actorId, UUID roleId) {
        Mono<Void> operation = requiredCustomRole(roleId)
            .flatMap(role -> userRoleRepository.countByRoleId(roleId)
                .flatMap(count -> count > 0
                    ? Mono.error(new ConflictException("角色仍被用户绑定，不能删除"))
                    : permissionRepository.deleteAllByRoleId(roleId)
                        .then(roleRepository.delete(role))
                        .then(emit("authorization.role.deleted", role.id(), rolePayload(role)))
                        .then(adminAudit(actorId, "identity.role.delete", "ROLE", role.id()))));
        return transactional(operation);
    }

    @Override
    public Flux<RoleView> list() {
        return roleRepository.findAll().sort(java.util.Comparator.comparing(PlatformRoleEntity::code))
            .take(MAX_UNPAGED_RESULTS)
            .flatMap(this::toView);
    }

    @Override
    public Mono<RoleView> grantPermission(UUID actorId, UUID roleId, PlatformPermission permission) {
        Instant now = Instant.now();
        Mono<RoleView> operation = requiredRole(roleId)
            .flatMap(role -> permissionRepository.findByRoleIdAndPermissionKey(roleId, permission.key())
                .hasElement()
                .flatMap(exists -> exists ? Mono.just(false) : permissionRepository.save(new RolePermissionEntity(
                    null, roleId, permission.key(), now, null
                )).thenReturn(true)))
            .flatMap(changed -> requiredRole(roleId).flatMap(this::toView)
                .flatMap(view -> (changed ? emit("authorization.role.permissions-replaced", roleId,
                    "{\"role_id\":\"" + roleId + "\",\"permission_keys\":"
                        + permissionKeysPayload(view.permissions()) + "}")
                    : Mono.empty()).then(adminAudit(actorId, "identity.role.permission.grant", "ROLE", roleId))
                    .thenReturn(view)));
        return transactional(operation);
    }

    @Override
    public Mono<RoleView> replacePermissions(UUID actorId, UUID roleId, ReplaceRolePermissionsRequest request) {
        List<String> desired = request.permissions().stream()
            .map(PlatformPermission::fromKey)
            .map(PlatformPermission::key)
            .distinct()
            .sorted()
            .toList();
        Mono<RoleView> operation = requiredRole(roleId)
            .flatMap(role -> permissionRepository.findAllByRoleId(roleId).map(RolePermissionEntity::permissionKey)
                .sort().collectList().flatMap(current -> {
                    if (current.equals(desired)) return toView(role);
                    Instant now = Instant.now();
                    return permissionRepository.deleteAllByRoleId(roleId)
                        .thenMany(Flux.fromIterable(desired)
                            .map(key -> new RolePermissionEntity(null, roleId, key, now, null))
                            .flatMap(permissionRepository::save))
                        .then(toView(role))
                        .flatMap(view -> emit("authorization.role.permissions-replaced", roleId,
                            "{\"role_id\":\"" + roleId + "\",\"permission_keys\":[\""
                                + String.join("\",\"", desired) + "\"]}").thenReturn(view));
                }))
            .flatMap(view -> adminAudit(actorId, "identity.role.permission.replace", "ROLE", roleId)
                .thenReturn(view));
        return transactional(operation);
    }

    @Override
    public Mono<Void> assignRole(UUID actorId, UUID userId, UUID roleId) {
        Instant now = Instant.now();
        Mono<Void> operation = requiredRole(roleId)
            .then(userRoleRepository.findByUserIdAndRoleId(userId, roleId))
            .switchIfEmpty(userRoleRepository.save(new UserRoleEntity(null, userId, roleId, now, null)))
            .flatMap(binding -> emit("authorization.user.role-assigned", userId,
                "{\"user_id\":\"" + userId + "\",\"role_id\":\"" + roleId + "\"}")
                .then(adminAudit(actorId, "identity.user.role.assign", "USER", userId,
                    "{\"role_id\":\"" + roleId + "\"}")))
            .then();
        return transactional(operation);
    }

    @Override
    public Mono<Void> revokeRole(UUID actorId, UUID userId, UUID roleId) {
        Mono<Void> operation = requiredRole(roleId)
            .then(userRoleRepository.deleteByUserIdAndRoleId(userId, roleId))
            .then(emit("authorization.user.role-removed", userId,
                "{\"user_id\":\"" + userId + "\",\"role_id\":\"" + roleId + "\"}"))
            .then(adminAudit(actorId, "identity.user.role.revoke", "USER", userId,
                "{\"role_id\":\"" + roleId + "\"}"));
        return transactional(operation);
    }

    private Mono<Void> emit(String type, UUID roleId, String payload) {
        return eventService == null ? Mono.empty() : eventService.append(new EventAppendRequest(type, 1, "authorization", "role", roleId, payload)).then();
    }

    private String permissionKeysPayload(List<String> permissions) {
        return "[\"" + String.join("\",\"", permissions) + "\"]";
    }

    private Mono<PlatformRoleEntity> requiredRole(UUID roleId) {
        return roleRepository.findById(roleId)
            .switchIfEmpty(Mono.error(new NotFoundException("角色不存在")));
    }

    private Mono<PlatformRoleEntity> requiredCustomRole(UUID roleId) {
        return requiredRole(roleId).flatMap(role -> role.builtIn()
            ? Mono.error(new ForbiddenException("内置角色不能修改或删除")) : Mono.just(role));
    }

    private Mono<Void> adminAudit(UUID actorId, String action, String targetType, UUID targetId) {
        return adminAudit(actorId, action, targetType, targetId, "{}");
    }

    private Mono<Void> adminAudit(UUID actorId, String action, String targetType, UUID targetId, String details) {
        return auditService.record(new AuditEventCommand(AuditActorType.ADMIN, actorId, action, targetType, targetId,
            AuditResult.SUCCESS, AuditRiskLevel.HIGH, details, 1, null));
    }

    private String rolePayload(PlatformRoleEntity role) {
        return "{\"role_id\":\"" + role.id() + "\",\"role_key\":\"" + role.code() + "\"}";
    }

    private <T> Mono<T> transactional(Mono<T> operation) {
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    private Mono<RoleView> toView(PlatformRoleEntity role) {
        return permissionRepository.findAllByRoleId(role.id())
            .map(RolePermissionEntity::permissionKey)
            .sort()
            .collectList()
            .map(permissions -> new RoleView(role.id(), role.code(), role.name(), role.description(), role.builtIn(),
                permissions));
    }
}
