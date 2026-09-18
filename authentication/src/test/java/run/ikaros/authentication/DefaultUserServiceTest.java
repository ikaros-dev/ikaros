package run.ikaros.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import run.ikaros.authorization.api.RoleMembershipQuery;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.ForbiddenException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.operations.api.AuditEventCommand;
import org.springframework.transaction.reactive.TransactionalOperator;

/** 验证平台用户服务的创建、查询与状态规则。 */
class DefaultUserServiceTest {
    private PlatformUserRepository userRepository;
    private RoleMembershipQuery roleMembershipQuery;
    private AuditService auditService;
    private PasswordCredentialRepository credentialRepository;
    private DefaultUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        roleMembershipQuery = mock(RoleMembershipQuery.class);
        when(roleMembershipQuery.roleCodesFor(any())).thenReturn(Mono.just(List.of()));
        auditService = mock(AuditService.class);
        credentialRepository = mock(PasswordCredentialRepository.class);
        when(userRepository.findIncludingDeletedByUsername(any())).thenReturn(Mono.empty());
        service = new DefaultUserService(userRepository, roleMembershipQuery, auditService, null, null,
            credentialRepository);
    }

    @Test
    void createsActiveUserWithPasswordAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity saved = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 0L);
        when(userRepository.save(any())).thenReturn(Mono.just(saved));
        when(credentialRepository.save(any())).thenReturn(Mono.just(mock(PasswordCredentialEntity.class)));
        when(auditService.record(eq(actorId), eq("identity.user.create"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(actorId,
                new CreateUserRequest("alice", "Alice", "Alice@Example.COM", "correct horse")))
            .assertNext(user -> {
                assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
                assertThat(user.email()).isEqualTo("alice@example.com");
            })
            .verifyComplete();
        verify(credentialRepository).save(argThat(credential -> credential.passwordHash().startsWith("pbkdf2-sha256$")
            && !credential.passwordHash().equals("correct horse")));
        verify(auditService).record(actorId, "identity.user.create", "USER", userId, "{}");
    }

    @Test
    void listsUsersWithStatusFilterAndPaging() {
        Instant now = Instant.now();
        PlatformUserEntity active = new PlatformUserEntity(UUID.randomUUID(), "alice", "Alice", null,
            UserStatus.ACTIVE, now.plusSeconds(1), now, null, 0L);
        PlatformUserEntity pending = new PlatformUserEntity(UUID.randomUUID(), "bob", "Bob", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        when(userRepository.findAll()).thenReturn(Flux.just(pending, active));

        StepVerifier.create(service.list(UserStatus.ACTIVE, "ali", 0, 20))
            .assertNext(result -> {
                assertThat(result.total()).isEqualTo(1);
                assertThat(result.items()).extracting(UserView::username).containsExactly("alice");
            })
            .verifyComplete();
    }

    @Test
    void changesUserStatusAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 1L);
        PlatformUserEntity locked = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.LOCKED, now, now, null, 2L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.just(locked));
        when(auditService.record(eq(actorId), eq("identity.user.status.change"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.changeStatus(actorId, userId, UserStatus.LOCKED))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.LOCKED))
            .verifyComplete();
        verify(auditService).record(actorId, "identity.user.status.change", "USER", userId, "{}");
    }

    @Test
    void restoresSoftDeletedUserAndReplacesProfileAndPassword() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity deleted = new PlatformUserEntity(userId, "alice", "Old Alice", "old@example.com",
            UserStatus.DEACTIVATED, now.minusSeconds(100), now.minusSeconds(10), null, 4L, 2L, 1);
        PlatformUserEntity restored = new PlatformUserEntity(userId, "alice", "New Alice", "new@example.com",
            UserStatus.ACTIVE, deleted.createdAt(), now, null, 5L, 3L, 0);
        PasswordCredentialEntity oldCredential = new PasswordCredentialEntity(UUID.randomUUID(), userId,
            "old-hash", deleted.createdAt(), deleted.updatedAt(), 1L);
        when(userRepository.findIncludingDeletedByUsername("alice")).thenReturn(Mono.just(deleted));
        when(userRepository.save(any())).thenReturn(Mono.just(restored));
        when(credentialRepository.findByUserId(userId)).thenReturn(Mono.just(oldCredential));
        when(credentialRepository.save(any())).thenReturn(Mono.just(oldCredential));
        when(auditService.record(eq(actorId), eq("identity.user.restore"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(actorId,
                new CreateUserRequest("alice", "New Alice", "New@Example.COM", "new password")))
            .assertNext(user -> {
                assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
                assertThat(user.displayName()).isEqualTo("New Alice");
                assertThat(user.email()).isEqualTo("new@example.com");
            })
            .verifyComplete();
        verify(userRepository).save(argThat(user -> user.isDel() == 0
            && user.status() == UserStatus.ACTIVE && user.securityVersion() == 5L));
        verify(credentialRepository).save(argThat(credential -> credential.id().equals(oldCredential.id())
            && credential.passwordHash().startsWith("pbkdf2-sha256$")
            && !credential.passwordHash().equals("new password")));
    }

    @Test
    void softDeletesUserAndInvalidatesExistingTokens() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity current = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 4L, 2L);
        PlatformUserEntity deleted = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.DEACTIVATED, now, now, null, 5L, 3L, 1);
        when(userRepository.findById(userId)).thenReturn(Mono.just(current));
        when(userRepository.save(any())).thenReturn(Mono.just(deleted));
        when(auditService.record(eq(actorId), eq("identity.user.delete"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.delete(actorId, userId))
            .verifyComplete();

        verify(userRepository).save(argThat(user -> user.status() == UserStatus.DEACTIVATED
            && user.securityVersion() == 5L && user.isDel() == 1));
        verify(auditService).record(actorId, "identity.user.delete", "USER", userId, "{}");
    }

    @Test
    void neverReturnsSoftDeletedUsers() {
        Instant now = Instant.now();
        PlatformUserEntity visible = new PlatformUserEntity(UUID.randomUUID(), "alice", "Alice", null,
            UserStatus.ACTIVE, now.plusSeconds(1), now, null, 0L);
        PlatformUserEntity deleted = new PlatformUserEntity(UUID.randomUUID(), "bob", "Bob", null,
            UserStatus.ACTIVE, now, now, null, 0L, null, 1);
        when(userRepository.findAll()).thenReturn(Flux.just(deleted, visible));

        StepVerifier.create(service.list(null, null, 0, 20))
            .assertNext(result -> {
                assertThat(result.total()).isEqualTo(1);
                assertThat(result.items()).extracting(UserView::username).containsExactly("alice");
            })
            .verifyComplete();
    }

    @Test
    void updatesUserProfileAndStatus() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity current = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 1L, 2L);
        PlatformUserEntity updated = new PlatformUserEntity(userId, "alice2", "Alice Two", "alice2@example.com",
            UserStatus.DISABLED, now, now.plusSeconds(1), null, 2L, 3L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(current));
        when(userRepository.save(any())).thenReturn(Mono.just(updated));
        when(auditService.record(eq(actorId), eq("identity.user.update"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.update(actorId, userId,
                new UpdateUserRequest("alice2", "Alice Two", "Alice2@Example.COM", UserStatus.DISABLED)))
            .assertNext(user -> {
                assertThat(user.username()).isEqualTo("alice2");
                assertThat(user.displayName()).isEqualTo("Alice Two");
                assertThat(user.email()).isEqualTo("alice2@example.com");
                assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
            })
            .verifyComplete();
        verify(userRepository).save(argThat(user -> user.securityVersion() == 2L
            && user.status() == UserStatus.DISABLED));
        verify(auditService).record(actorId, "identity.user.update", "USER", userId, "{}");
    }

    @Test
    void rejectsUpdatingCurrentUser() {
        UUID actorId = UUID.randomUUID();

        StepVerifier.create(service.update(actorId, actorId,
                new UpdateUserRequest("alice", "Alice", "alice@example.com", UserStatus.ACTIVE)))
            .expectError(ForbiddenException.class)
            .verify();
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void rejectsDeletingCurrentUser() {
        UUID actorId = UUID.randomUUID();

        StepVerifier.create(service.delete(actorId, actorId))
            .expectError(ForbiddenException.class)
            .verify();
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void invalidatesAllUserTokensByAtomicallyIncreasingSecurityVersion() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity current = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 4L, 2L);
        PlatformUserEntity saved = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 5L, 3L);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Mono.just(current));
        when(userRepository.save(any())).thenReturn(Mono.just(saved));
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.just(mock(run.ikaros.integration.api.EventReference.class)));
        when(auditService.record(eq(actorId), eq("identity.user.tokens.invalidate"), eq("USER"), eq(userId),
            eq("{\"security_version\":5}"))).thenReturn(Mono.empty());

        DefaultUserService invalidating = new DefaultUserService(userRepository, roleMembershipQuery, auditService,
            events, transaction);
        StepVerifier.create(invalidating.invalidateTokens(actorId, userId))
            .assertNext(result -> assertThat(result.securityVersion()).isEqualTo(5L))
            .verifyComplete();
        verify(transaction).transactional(any(Mono.class));
        verify(events).append(argThat(request -> request.eventType().equals("authentication.user.tokens-invalidated")
            && request.payloadJson().contains("\"security_version\":5")
            && !request.payloadJson().contains("token")));
    }

    @Test
    void recordsOwnTokenInvalidationAsSensitiveUserSecurityOperation() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity current = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 4L, 2L);
        PlatformUserEntity saved = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 5L, 3L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(current));
        when(userRepository.save(any())).thenReturn(Mono.just(saved));
        when(auditService.record(any(AuditEventCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.invalidateTokens(userId, userId))
            .assertNext(result -> assertThat(result.securityVersion()).isEqualTo(5L))
            .verifyComplete();

        verify(auditService).record(org.mockito.ArgumentMatchers.argThat(event ->
            event.actorType() == run.ikaros.operations.api.AuditActorType.USER
                && event.riskLevel() == run.ikaros.operations.api.AuditRiskLevel.SENSITIVE
                && event.result() == run.ikaros.operations.api.AuditResult.SUCCESS));
    }

    @Test
    void rejectsInvalidPagingBeforeQuery() {
        StepVerifier.create(service.list(null, null, -1, 20))
            .expectError(IllegalArgumentException.class)
            .verify();
        StepVerifier.create(service.list(null, null, 0, 101))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void emitsNonSensitiveEventWhenUserIsDisabled() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 1L);
        PlatformUserEntity disabled = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.DISABLED, now, now, null, 3L);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.just(disabled));
        when(events.append(any(EventAppendRequest.class)))
            .thenReturn(Mono.empty());
        when(auditService.record(eq(actorId), eq("identity.user.status.change"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());
        DefaultUserService eventService = new DefaultUserService(userRepository, roleMembershipQuery, auditService, events);

        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.DISABLED))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.DISABLED))
            .verifyComplete();
        verify(events).append(argThat(request -> request.eventType().equals("authentication.user.disabled")
            && request.producerSubsystem().equals("authentication") && request.subjectType().equals("user")
            && request.subjectId().equals(userId)));
    }

    @Test
    void emitsCreatedAndEnabledLifecycleEvents() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        PlatformUserEntity created = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 0L);
        PlatformUserEntity disabled = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.DISABLED, now, now, null, 1L);
        PlatformUserEntity enabled = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 2L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(created), Mono.just(disabled));
        when(userRepository.save(any())).thenReturn(Mono.just(created), Mono.just(disabled), Mono.just(enabled));
        when(events.append(any(EventAppendRequest.class)))
            .thenReturn(Mono.empty());
        when(auditService.record(any(), any(String.class), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());
        PasswordCredentialRepository eventCredentials = mock(PasswordCredentialRepository.class);
        when(eventCredentials.save(any())).thenReturn(Mono.just(mock(PasswordCredentialEntity.class)));
        DefaultUserService eventService = new DefaultUserService(userRepository, roleMembershipQuery, auditService,
            events, null, eventCredentials);

        StepVerifier.create(eventService.create(actorId,
                new CreateUserRequest("alice", "Alice", "alice@example.com", "correct horse")))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.ACTIVE)).verifyComplete();
        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.DISABLED)).expectNextCount(1).verifyComplete();
        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.ACTIVE)).expectNextCount(1).verifyComplete();
        verify(events).append(argThat(request -> request.eventType().equals("authentication.user.created")
            && request.producerSubsystem().equals("authentication") && request.subjectType().equals("user")
            && request.subjectId().equals(userId)));
        verify(events).append(argThat(request -> request.eventType().equals("authentication.user.enabled")
            && request.producerSubsystem().equals("authentication") && request.subjectType().equals("user")
            && request.subjectId().equals(userId)));
    }

}
