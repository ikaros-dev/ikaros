package run.ikaros.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.authorization.api.InitialRoleAssigner;
import run.ikaros.authorization.api.PermissionSnapshot;
import run.ikaros.authorization.api.PermissionSnapshotQuery;

class AuthenticationServiceTest {
    @Test
    void initializesFirstUserWithHashedPasswordAndAdminRoleAtomically() {
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
        UserService userService = mock(UserService.class);
        JwtTokenService tokens = mock(JwtTokenService.class);
        InitialRoleAssigner roles = mock(InitialRoleAssigner.class);
        PermissionSnapshotQuery permissions = mock(PermissionSnapshotQuery.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        when(users.save(any())).thenReturn(Mono.just(user));
        when(credentials.save(any())).thenReturn(Mono.just(mock(PasswordCredentialEntity.class)));
        when(users.count()).thenReturn(Mono.just(1L));
        when(roles.assignInitialRole(userId, "admin")).thenReturn(Mono.empty());
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.get(userId)).thenReturn(Mono.just(new UserView(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, List.of("admin"), now, null)));
        when(permissions.permissionsFor(userId)).thenReturn(Mono.just(new PermissionSnapshot(userId, List.of("system.user.manage"))));
        when(tokens.issue(userId, 0L, List.of("system.user.manage")))
            .thenReturn(new JwtTokenService.TokenPair("access", "refresh", now.plusSeconds(300)));

        AuthenticationService service = new AuthenticationService(users, credentials, userService, tokens, roles,
            permissions, transaction);
        StepVerifier.create(service.register(new RegisterRequest("admin", "correct horse battery staple",
                "Administrator", null)))
            .assertNext(view -> assertThat(view.userId()).isEqualTo(userId))
            .verifyComplete();

        verify(transaction).transactional(any(Mono.class));
        verify(roles).assignInitialRole(userId, "admin");
        org.mockito.ArgumentCaptor<PasswordCredentialEntity> captured = org.mockito.ArgumentCaptor.forClass(PasswordCredentialEntity.class);
        verify(credentials).save(captured.capture());
        assertThat(captured.getValue().passwordHash()).startsWith("pbkdf2-sha256$")
            .doesNotContain("correct horse battery staple");
    }

    @Test
    void rejectsInvalidInitializationInputBeforePersistence() {
        AuthenticationService service = new AuthenticationService(mock(PlatformUserRepository.class),
            mock(PasswordCredentialRepository.class), mock(UserService.class), mock(JwtTokenService.class),
            mock(InitialRoleAssigner.class), mock(PermissionSnapshotQuery.class), mock(TransactionalOperator.class));
        StepVerifier.create(service.register(new RegisterRequest("", "short", "", null)))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void loginIssuesStatelessTokenPairAndLogoutDoesNotRevokeServerSession() {
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
        UserService userService = mock(UserService.class);
        JwtTokenService tokens = mock(JwtTokenService.class);
        PermissionSnapshotQuery permissions = mock(PermissionSnapshotQuery.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        String password = "correct horse battery staple";
        when(users.findByUsername("admin")).thenReturn(Mono.just(user));
        org.mockito.ArgumentCaptor<PasswordCredentialEntity> captured = org.mockito.ArgumentCaptor.forClass(PasswordCredentialEntity.class);
        when(credentials.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(credentials.findByUserId(userId)).thenAnswer(invocation -> Mono.just(captured.getValue()));
        when(users.count()).thenReturn(Mono.just(2L));
        when(userService.get(userId)).thenReturn(Mono.just(new UserView(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, List.of("admin"), now, null)));
        when(permissions.permissionsFor(userId)).thenReturn(Mono.just(new PermissionSnapshot(userId, List.of())));
        when(tokens.issue(userId, 0L, List.of())).thenReturn(new JwtTokenService.TokenPair("access", "refresh", now.plusSeconds(300)));
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthenticationService service = new AuthenticationService(users, credentials, userService, tokens,
            mock(InitialRoleAssigner.class), permissions, transaction);

        when(users.save(any())).thenReturn(Mono.just(user));
        when(userService.get(userId)).thenReturn(Mono.just(new UserView(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, List.of("admin"), now, null)));
        when(tokens.issue(userId, 0L, List.of())).thenReturn(new JwtTokenService.TokenPair("access", "refresh", now.plusSeconds(300)));
        StepVerifier.create(service.register(new RegisterRequest("admin", password, "Administrator", null)))
            .expectNextCount(1).verifyComplete();
        verify(credentials).save(captured.capture());

        StepVerifier.create(service.login(new LoginRequest("admin", password)))
            .assertNext(view -> assertThat(view).extracting(AuthenticationView::userId,
                AuthenticationView::accessToken, AuthenticationView::refreshToken)
                .containsExactly(userId, "access", "refresh"))
            .verifyComplete();
        assertThat(AuthenticationView.class.getRecordComponents()).extracting(component -> component.getName())
            .doesNotContain("sessionId");

        StepVerifier.create(service.logout()).verifyComplete();
    }
}
