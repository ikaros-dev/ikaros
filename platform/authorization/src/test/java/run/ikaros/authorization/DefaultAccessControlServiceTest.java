package run.ikaros.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.authorization.api.PlatformPermission;
import run.ikaros.authentication.api.SecurityVerificationLevel;
import run.ikaros.common.ForbiddenException;

/** 验证权限与安全验证等级必须同时成立。 */
class DefaultAccessControlServiceTest {
    private UserRoleRepository userRoleRepository;
    private RolePermissionRepository permissionRepository;
    private DefaultAccessControlService service;

    @BeforeEach
    void setUp() {
        userRoleRepository = mock(UserRoleRepository.class);
        permissionRepository = mock(RolePermissionRepository.class);
        service = new DefaultAccessControlService(userRoleRepository, permissionRepository);
    }

    @Test
    void requiresActiveUserPermissionAndFreshSvlTogether() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.just(new UserRoleEntity(UUID.randomUUID(), userId,
            roleId, now, 0L)));
        when(permissionRepository.findByRoleIdAndPermissionKey(roleId, PlatformPermission.RESOURCE_DELETE.key()))
            .thenReturn(Mono.just(new RolePermissionEntity(UUID.randomUUID(), roleId,
                PlatformPermission.RESOURCE_DELETE.key(), now, 0L)));
        SecurityPolicy policy = new SecurityPolicy("DELETE_RESOURCE", PlatformPermission.RESOURCE_DELETE,
            SecurityVerificationLevel.SVL_1, true);

        StepVerifier.create(service.require(userId, SecurityVerificationLevel.SVL_1,
            now.plusSeconds(300), policy)).verifyComplete();
    }

    @Test
    void rejectsHighSvlTokenWithoutRolePermission() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        StepVerifier.create(service.require(userId, SecurityVerificationLevel.SVL_4, now.plusSeconds(300),
            new SecurityPolicy("MANAGE_USERS",
                PlatformPermission.SYSTEM_USER_MANAGE, SecurityVerificationLevel.SVL_1, true)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ForbiddenException.class);
                assertThat(error).hasMessage("当前身份、权限或安全验证等级不满足操作要求");
            })
            .verify();
    }

    @Test
    void rejectsExpiredVerification() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID roleId = UUID.randomUUID();
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.just(new UserRoleEntity(UUID.randomUUID(), userId,
            roleId, now, 0L)));
        when(permissionRepository.findByRoleIdAndPermissionKey(roleId, PlatformPermission.RESOURCE_READ.key()))
            .thenReturn(Mono.just(new RolePermissionEntity(UUID.randomUUID(), roleId,
                PlatformPermission.RESOURCE_READ.key(), now, 0L)));
        StepVerifier.create(service.require(userId, SecurityVerificationLevel.SVL_0, null,
            new SecurityPolicy("READ",
                PlatformPermission.RESOURCE_READ, SecurityVerificationLevel.SVL_0, true)))
            .expectError(ForbiddenException.class)
            .verify();
    }
}
