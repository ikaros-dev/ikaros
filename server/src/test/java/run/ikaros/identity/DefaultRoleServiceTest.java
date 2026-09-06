package run.ikaros.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;

/** 验证角色与平台权限注册表的关键业务规则。 */
class DefaultRoleServiceTest {
    private PlatformRoleRepository roleRepository;
    private RolePermissionRepository permissionRepository;
    private AuditService auditService;
    private DefaultRoleService service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(PlatformRoleRepository.class);
        permissionRepository = mock(RolePermissionRepository.class);
        auditService = mock(AuditService.class);
        service = new DefaultRoleService(roleRepository, permissionRepository, auditService);
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
}
