package run.ikaros.authorization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.authorization.api.PlatformPermission;
import run.ikaros.common.ConflictException;
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
    private final AuditService auditService;
    private final DurableEventPublisher eventService;

    /**
     * 创建角色服务。
     *
     * @param roleRepository 角色仓储
     * @param permissionRepository 角色权限绑定仓储
     * @param auditService 审计服务
     */
    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService) {
        this(roleRepository, permissionRepository, auditService, null);
    }

    @Autowired
    public DefaultRoleService(PlatformRoleRepository roleRepository, RolePermissionRepository permissionRepository,
                              AuditService auditService, DurableEventPublisher eventService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
        this.eventService = eventService;
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
        return roleRepository.save(role)
            .onErrorMap(DuplicateKeyException.class, exception -> new ConflictException("角色编码已存在"))
            .flatMap(saved -> emit("authorization.role.created", saved.id(),
                    "{\"role_id\":\"" + saved.id() + "\",\"role_key\":\"" + saved.code() + "\"}")
                .then(auditService.record(actorId, "identity.role.create", "ROLE", saved.id(), "{}"))
                .then(toView(saved)));
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
        return requiredRole(roleId)
            .flatMap(role -> permissionRepository.findByRoleIdAndPermissionKey(roleId, permission.key())
                .hasElement()
                .flatMap(exists -> exists ? Mono.just(false) : permissionRepository.save(new RolePermissionEntity(
                    null, roleId, permission.key(), now, null
                )).thenReturn(true)))
            .flatMap(changed -> requiredRole(roleId).flatMap(this::toView)
                .flatMap(view -> (changed ? emit("authorization.role.permissions-replaced", roleId,
                    "{\"role_id\":\"" + roleId + "\",\"permission_keys\":"
                        + permissionKeysPayload(view.permissions()) + "}")
                    : Mono.empty()).then(auditService.record(actorId, "identity.role.permission.grant", "ROLE", roleId, "{}"))
                    .thenReturn(view)));
    }

    @Override
    public Mono<RoleView> replacePermissions(UUID actorId, UUID roleId, ReplaceRolePermissionsRequest request) {
        List<String> desired = request.permissions().stream().map(PlatformPermission::key).distinct().sorted().toList();
        return requiredRole(roleId)
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
            .flatMap(view -> auditService.record(actorId, "identity.role.permission.replace", "ROLE", roleId, "{}")
                .thenReturn(view));
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

    private Mono<RoleView> toView(PlatformRoleEntity role) {
        return permissionRepository.findAllByRoleId(role.id())
            .map(RolePermissionEntity::permissionKey)
            .sort()
            .collectList()
            .map(permissions -> new RoleView(role.id(), role.code(), role.name(), role.description(), role.builtIn(),
                permissions));
    }
}
