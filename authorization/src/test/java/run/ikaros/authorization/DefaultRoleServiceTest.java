package run.ikaros.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.authorization.api.PlatformPermission;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.common.ConflictException;
import run.ikaros.common.ForbiddenException;

/** 验证角色与平台权限注册表的关键业务规则。 */
class DefaultRoleServiceTest {
    private PlatformRoleRepository roleRepository;
    private RolePermissionRepository permissionRepository;
    private UserRoleRepository userRoleRepository;
    private AuditService auditService;
    private DefaultRoleService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(PlatformRoleRepository.class);
        permissionRepository = mock(RolePermissionRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        auditService = mock(AuditService.class);
        when(auditService.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());
        service = new DefaultRoleService(roleRepository, permissionRepository, auditService, null, userRoleRepository);
    }

    @Test
    void createsCustomRoleAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity saved = new PlatformRoleEntity(roleId, "CONTENT_ADMIN", "内容管理员", "管理内容",
            false, now, now, 0L);
        when(roleRepository.save(any())).thenReturn(Mono.just(saved));
        when(permissionRepository.findAllByRoleId(roleId)).thenReturn(Flux.empty());
        when(auditService.record(eq(actorId), eq("identity.role.create"), eq("ROLE"), eq(roleId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(actorId, new CreateRoleRequest("CONTENT_ADMIN", "内容管理员", "管理内容")))
            .assertNext(view -> assertThat(view.code()).isEqualTo("CONTENT_ADMIN"))
            .verifyComplete();
    }

    @Test
    void rejectsInvalidRoleInputBeforePersistence() {
        StepVerifier.create(service.create(UUID.randomUUID(), new CreateRoleRequest("invalid-code", "", null)))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void listsRolesWithGrantedPermissions() {
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "AUDITOR", "审计员", null, true, now, now, 0L);
        when(roleRepository.findAll()).thenReturn(Flux.just(role));
        when(permissionRepository.findAllByRoleId(roleId)).thenReturn(Flux.just(new RolePermissionEntity(
            UUID.randomUUID(), roleId, PlatformPermission.SYSTEM_AUDIT_READ.key(), now, 0L
        )));

        StepVerifier.create(service.list())
            .assertNext(view -> assertThat(view.permissions()).containsExactly("system.audit.read"))
            .verifyComplete();
    }

    @Test
    void unpagedRoleListIsBounded() {
        Instant now = Instant.now();
        when(roleRepository.findAll()).thenReturn(Flux.range(0, 101).map(i -> new PlatformRoleEntity(
            UUID.randomUUID(), "ROLE_" + String.format("%03d", i), "Role " + i, null, false, now, now, 0L)));
        when(permissionRepository.findAllByRoleId(any())).thenReturn(Flux.empty());

        StepVerifier.create(service.list().count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    void grantsOnlyDeclaredPermissionAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "USER_ADMIN", "用户管理员", null,
            false, now, now, 0L);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(permissionRepository.findByRoleIdAndPermissionKey(roleId, PlatformPermission.SYSTEM_USER_MANAGE.key()))
            .thenReturn(Mono.empty());
        when(permissionRepository.save(any())).thenReturn(Mono.just(new RolePermissionEntity(UUID.randomUUID(), roleId,
            PlatformPermission.SYSTEM_USER_MANAGE.key(), now, 0L)));
        when(permissionRepository.findAllByRoleId(roleId)).thenReturn(Flux.just(new RolePermissionEntity(UUID.randomUUID(),
            roleId, PlatformPermission.SYSTEM_USER_MANAGE.key(), now, 0L)));
        when(auditService.record(eq(actorId), eq("identity.role.permission.grant"), eq("ROLE"), eq(roleId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.grantPermission(actorId, roleId, PlatformPermission.SYSTEM_USER_MANAGE))
            .assertNext(view -> assertThat(view.permissions()).containsExactly("system.user.manage"))
            .verifyComplete();
        verify(permissionRepository).save(any(RolePermissionEntity.class));
    }

    @Test
    void replacesPermissionsUsingRegisteredRoutePermissionKeys() {
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "CONSOLE_READER", "控制台查看者", null,
            false, now, now, 0L);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(permissionRepository.findAllByRoleId(roleId)).thenReturn(Flux.empty(), Flux.just(
            new RolePermissionEntity(UUID.randomUUID(), roleId, "user.read", now, 0L)));
        when(permissionRepository.deleteAllByRoleId(roleId)).thenReturn(Mono.empty());
        when(permissionRepository.save(any())).thenReturn(Mono.just(new RolePermissionEntity(
            UUID.randomUUID(), roleId, "user.read", now, 0L)));
        when(auditService.record(eq(actorId), eq("identity.role.permission.replace"), eq("ROLE"), eq(roleId), eq("{}")))
            .thenReturn(Mono.empty());

        ReplaceRolePermissionsRequest request = new ReplaceRolePermissionsRequest(
            List.of("user.read"));

        StepVerifier.create(service.replacePermissions(actorId, roleId, request))
            .assertNext(view -> assertThat(view.permissions()).containsExactly("user.read"))
            .verifyComplete();
    }

    @Test
    void assignsAndRevokesUserRoleWithoutCrossUserSideEffects() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "EDITOR", "Editor", null, false, now, now, 0L);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId)).thenReturn(Mono.empty());
        when(userRoleRepository.save(any())).thenReturn(Mono.just(new UserRoleEntity(UUID.randomUUID(), userId, roleId, now, 0L)));
        when(auditService.record(eq(actorId), eq("identity.user.role.assign"), eq("USER"), eq(userId), any()))
            .thenReturn(Mono.empty());
        when(userRoleRepository.deleteByUserIdAndRoleId(userId, roleId)).thenReturn(Mono.empty());
        when(auditService.record(eq(actorId), eq("identity.user.role.revoke"), eq("USER"), eq(userId), any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.assignRole(actorId, userId, roleId)).verifyComplete();
        StepVerifier.create(service.revokeRole(actorId, userId, roleId)).verifyComplete();
        verify(userRoleRepository).save(any(UserRoleEntity.class));
        verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
    }

    @Test
    void updatesCustomRoleAndWritesHighRiskAdminAudit() {
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "EDITOR", "Editor", null, false, now, now, 0L);
        PlatformRoleEntity updated = new PlatformRoleEntity(roleId, "EDITOR", "Content editor", "May edit content",
            false, now, now.plusSeconds(1), 1L);
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(roleRepository.save(any())).thenReturn(Mono.just(updated));
        when(permissionRepository.findAllByRoleId(roleId)).thenReturn(Flux.empty());

        StepVerifier.create(service.update(actorId, roleId, new UpdateRoleRequest("Content editor", "May edit content")))
            .assertNext(view -> assertThat(view.name()).isEqualTo("Content editor"))
            .verifyComplete();

        verify(auditService).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == AuditActorType.ADMIN && actorId.equals(event.actorId())
                && "identity.role.update".equals(event.action()) && roleId.equals(event.targetId())
                && event.result() == AuditResult.SUCCESS && event.riskLevel() == AuditRiskLevel.HIGH));
    }

    @Test
    void rejectsBuiltInOrAssignedRoleDeletion() {
        UUID actorId = UUID.randomUUID();
        UUID builtInId = UUID.randomUUID();
        UUID assignedId = UUID.randomUUID();
        Instant now = Instant.now();
        when(roleRepository.findById(builtInId)).thenReturn(Mono.just(new PlatformRoleEntity(builtInId, "ADMIN",
            "Administrator", null, true, now, now, 0L)));
        when(roleRepository.findById(assignedId)).thenReturn(Mono.just(new PlatformRoleEntity(assignedId, "EDITOR",
            "Editor", null, false, now, now, 0L)));
        when(userRoleRepository.countByRoleId(assignedId)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.delete(actorId, builtInId)).expectError(ForbiddenException.class).verify();
        StepVerifier.create(service.delete(actorId, assignedId)).expectError(ConflictException.class).verify();
    }
}
