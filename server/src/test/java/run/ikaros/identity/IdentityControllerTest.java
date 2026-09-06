package run.ikaros.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/** 验证身份与访问管理控制器的 HTTP 合约。 */
class IdentityControllerTest {
    private UserService userService;
    private RoleService roleService;
    private SecuritySessionService sessionService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        roleService = mock(RoleService.class);
        sessionService = mock(SecuritySessionService.class);
        client = WebTestClient.bindToController(new UserController(userService), new RoleController(roleService),
            new SecuritySessionController(sessionService)).build();
    }

    @Test
    void exposesUserCreationListingStatusAndRoleBindingEndpoints() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserView user = new UserView(userId, "alice", "Alice", "alice@example.com", UserStatus.PENDING,
            List.of(), Instant.now(), null);
        when(userService.create(any(), any())).thenReturn(Mono.just(user));
        when(userService.list(any(), any(), any(Integer.class), any(Integer.class)))
            .thenReturn(Mono.just(new PageResponse<>(List.of(user), 1, 0, 20)));
        when(userService.changeStatus(any(), any(), any())).thenReturn(Mono.just(user));
        when(userService.assignRole(any(), any(), any())).thenReturn(Mono.empty());

        client.post().uri("/api/users").header("X-Ikaros-Actor-Id", actorId.toString())
            .bodyValue(Map.of("username", "alice", "displayName", "Alice", "email", "alice@example.com"))
            .exchange().expectStatus().isCreated().expectHeader().valueEquals("Location", "/api/users/" + userId);
        client.get().uri("/api/users?status=PENDING&query=ali").exchange().expectStatus().isOk();
        client.post().uri("/api/users/{userId}/status/{status}", userId, UserStatus.ACTIVE)
            .header("X-Ikaros-Actor-Id", actorId.toString()).exchange().expectStatus().isOk();
        client.post().uri("/api/users/{userId}/roles/{roleId}", userId, roleId)
            .header("X-Ikaros-Actor-Id", actorId.toString()).exchange().expectStatus().isNoContent();
        verify(userService).assignRole(actorId, userId, roleId);
    }

    @Test
    void exposesRoleCreationListingAndPermissionGrantEndpoints() {
        UUID actorId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        RoleView role = new RoleView(roleId, "AUDITOR", "审计员", null, false, List.of());
        when(roleService.create(any(), any())).thenReturn(Mono.just(role));
        when(roleService.list()).thenReturn(Flux.just(role));
        when(roleService.grantPermission(any(), any(), any())).thenReturn(Mono.just(role));

        client.post().uri("/api/roles").header("X-Ikaros-Actor-Id", actorId.toString())
            .bodyValue(Map.of("code", "AUDITOR", "name", "审计员", "description", "审计"))
            .exchange().expectStatus().isCreated().expectHeader().valueEquals("Location", "/api/roles/" + roleId);
        client.get().uri("/api/roles").exchange().expectStatus().isOk();
        client.post().uri("/api/roles/{roleId}/permissions/{permission}", roleId,
                PlatformPermission.SYSTEM_AUDIT_READ)
            .header("X-Ikaros-Actor-Id", actorId.toString()).exchange().expectStatus().isOk();
        verify(roleService).grantPermission(actorId, roleId, PlatformPermission.SYSTEM_AUDIT_READ);
    }

    @Test
    void exposesActiveSessionListingAndScopedRevocationEndpoints() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        SessionView session = new SessionView(sessionId, userId, "EMAIL_OTP", SecurityVerificationLevel.SVL_1,
            Instant.now(), Instant.now().plusSeconds(300), Instant.now().plusSeconds(3600), Instant.now());
        when(sessionService.listActive(userId)).thenReturn(Flux.just(session));
        when(sessionService.revoke(actorId, userId, sessionId)).thenReturn(Mono.empty());

        client.get().uri("/api/users/{userId}/sessions", userId).exchange().expectStatus().isOk();
        client.delete().uri("/api/users/{userId}/sessions/{sessionId}", userId, sessionId)
            .header("X-Ikaros-Actor-Id", actorId.toString()).exchange().expectStatus().isNoContent();
        verify(sessionService).revoke(actorId, userId, sessionId);
    }
}
