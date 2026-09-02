package run.ikaros.identity;

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
import run.ikaros.common.ConflictException;

/** 验证权限与安全验证等级必须同时成立。 */
class DefaultAccessControlServiceTest {
    private PlatformUserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RolePermissionRepository permissionRepository;
    private SecuritySessionRepository sessionRepository;
    private DefaultAccessControlService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        permissionRepository = mock(RolePermissionRepository.class);
        sessionRepository = mock(SecuritySessionRepository.class);
        service = new DefaultAccessControlService(userRepository, userRoleRepository, permissionRepository,
            sessionRepository);
    }

    @Test
    void requiresActiveUserPermissionAndFreshSvlTogether() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRepository.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 0L)));
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.just(new UserRoleEntity(UUID.randomUUID(), userId,
            roleId, now, 0L)));
        when(permissionRepository.findByRoleIdAndPermissionKey(roleId, PlatformPermission.RESOURCE_DELETE.key()))
            .thenReturn(Mono.just(new RolePermissionEntity(UUID.randomUUID(), roleId,
                PlatformPermission.RESOURCE_DELETE.key(), now, 0L)));
        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(new SecuritySessionEntity(sessionId, userId,
            "EMAIL_OTP", 1, now, now.plusSeconds(300), now.plusSeconds(3600), null, now, now, 0L)));
        SecurityPolicy policy = new SecurityPolicy("DELETE_RESOURCE", PlatformPermission.RESOURCE_DELETE,
            SecurityVerificationLevel.SVL_1, true);

        StepVerifier.create(service.require(userId, sessionId, policy)).verifyComplete();
    }

    @Test
    void rejectsHighSvlSessionWithoutRolePermission() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRepository.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 0L)));
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(new SecuritySessionEntity(sessionId, userId,
            "EMAIL_OTP", 4, now, now.plusSeconds(300), now.plusSeconds(3600), null, now, now, 0L)));

        StepVerifier.create(service.require(userId, sessionId, new SecurityPolicy("MANAGE_USERS",
                PlatformPermission.SYSTEM_USER_MANAGE, SecurityVerificationLevel.SVL_1, true)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ConflictException.class);
                assertThat(error).hasMessage("当前身份、权限或安全验证等级不满足操作要求");
            })
            .verify();
    }
}
