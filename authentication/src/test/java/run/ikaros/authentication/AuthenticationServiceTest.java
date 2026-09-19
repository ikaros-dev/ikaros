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
import run.ikaros.common.NotFoundException;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.operations.api.AuditService;

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
        AuditService audit = mock(AuditService.class);
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
        when(audit.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());

        AuthenticationService service = new AuthenticationService(users, credentials, userService, tokens, roles,
            permissions, transaction, audit);
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
        verify(audit).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == AuditActorType.USER && event.result() == AuditResult.SUCCESS
                && event.riskLevel() == AuditRiskLevel.SENSITIVE
                && "authentication.register".equals(event.action())));
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
        AuditService audit = mock(AuditService.class);
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
        when(audit.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());
        AuthenticationService service = new AuthenticationService(users, credentials, userService, tokens,
            mock(InitialRoleAssigner.class), permissions, transaction, audit);

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
        verify(audit).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == AuditActorType.USER && event.result() == AuditResult.SUCCESS
                && "authentication.login".equals(event.action())));

        StepVerifier.create(service.logout()).verifyComplete();
    }

    @Test
    void recordsAnonymousFailureWithoutPersistingPassword() {
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        AuditService audit = mock(AuditService.class);
        when(users.findByUsername("unknown-user")).thenReturn(Mono.empty());
        when(audit.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());
        AuthenticationService service = new AuthenticationService(users, mock(PasswordCredentialRepository.class),
            mock(UserService.class), mock(JwtTokenService.class), mock(InitialRoleAssigner.class),
            mock(PermissionSnapshotQuery.class), mock(TransactionalOperator.class), audit);

        StepVerifier.create(service.login(new LoginRequest("unknown-user", "plain-password")))
            .expectError(NotFoundException.class)
            .verify();

        verify(audit).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == AuditActorType.ANONYMOUS && event.actorId() == null
                && event.result() == AuditResult.FAILURE && event.riskLevel() == AuditRiskLevel.SENSITIVE
                && event.detailsJson().contains("unknown-user") && !event.detailsJson().contains("plain-password")));
    }

    @Test
    void recordsAnonymousDeniedLoginForInactiveUser() {
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        PasswordCredentialRepository credentials = mock(PasswordCredentialRepository.class);
        AuditService audit = mock(AuditService.class);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity inactiveUser = new PlatformUserEntity(userId, "inactive-user", "Inactive", null,
            UserStatus.DISABLED, now, now, null, 0L);
        String password = "correct horse battery staple";
        when(users.findByUsername("inactive-user")).thenReturn(Mono.just(inactiveUser));
        when(credentials.findByUserId(userId)).thenReturn(Mono.just(new PasswordCredentialEntity(UUID.randomUUID(),
            userId, PasswordHashService.hash(password), now, now, 0L)));
        when(audit.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());
        AuthenticationService service = new AuthenticationService(users, credentials, mock(UserService.class),
            mock(JwtTokenService.class), mock(InitialRoleAssigner.class), mock(PermissionSnapshotQuery.class),
            mock(TransactionalOperator.class), audit);

        StepVerifier.create(service.login(new LoginRequest("inactive-user", password)))
            .expectError(NotFoundException.class)
            .verify();

        verify(audit).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == AuditActorType.ANONYMOUS && event.actorId() == null
                && "authentication.login".equals(event.action()) && "USER".equals(event.targetType())
                && userId.equals(event.targetId()) && event.result() == AuditResult.DENIED
                && event.riskLevel() == AuditRiskLevel.SENSITIVE));
    }

    @Test
    void refreshRejectsSecurityVersionMismatchAndInvalidRefreshToken() {
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        JwtTokenService tokens = mock(JwtTokenService.class);
        AuthenticationService service = new AuthenticationService(users, mock(PasswordCredentialRepository.class),
            mock(UserService.class), tokens, mock(InitialRoleAssigner.class), mock(PermissionSnapshotQuery.class),
            mock(TransactionalOperator.class));
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "admin", "Administrator", null,
            UserStatus.ACTIVE, now, now, null, 2L);
        when(tokens.verifyRefresh("refresh")).thenReturn(new JwtTokenService.Claims(userId, UUID.randomUUID(),
            1L, List.of(), now.plusSeconds(300)));
        when(users.findById(userId)).thenReturn(Mono.just(user));

        StepVerifier.create(service.refresh("refresh"))
            .expectError(NotFoundException.class)
            .verify();

        when(tokens.verifyRefresh("invalid")).thenThrow(new IllegalArgumentException("invalid"));
        StepVerifier.create(service.refresh("invalid"))
            .expectError(NotFoundException.class)
            .verify();
    }
}
