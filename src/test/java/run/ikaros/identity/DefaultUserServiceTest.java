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
import run.ikaros.event.DurableEventService;

/** 验证平台用户服务的创建、查询、状态与角色绑定规则。 */
class DefaultUserServiceTest {
    private PlatformUserRepository userRepository;
    private PlatformRoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private AuditService auditService;
    private DefaultUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        roleRepository = mock(PlatformRoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        auditService = mock(AuditService.class);
        service = new DefaultUserService(userRepository, roleRepository, userRoleRepository, auditService);
    }

    @Test
    void createsPendingUserAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity saved = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.PENDING, now, now, null, 0L);
        when(userRepository.save(any())).thenReturn(Mono.just(saved));
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        when(auditService.record(eq(actorId), eq("identity.user.create"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(actorId, new CreateUserRequest("alice", "Alice", "Alice@Example.COM")))
            .assertNext(user -> {
                assertThat(user.status()).isEqualTo(UserStatus.PENDING);
                assertThat(user.email()).isEqualTo("alice@example.com");
            })
            .verifyComplete();
        verify(auditService).record(actorId, "identity.user.create", "USER", userId, "{}");
    }

    @Test
    void listsUsersWithStatusFilterAndPaging() {
        Instant now = Instant.now();
        PlatformUserEntity active = new PlatformUserEntity(UUID.randomUUID(), "alice", "Alice", null,
            UserStatus.ACTIVE, now.plusSeconds(1), now, null, 0L);
        PlatformUserEntity pending = new PlatformUserEntity(UUID.randomUUID(), "bob", "Bob", null,
            UserStatus.PENDING, now, now, null, 0L);
        when(userRepository.findAll()).thenReturn(Flux.just(pending, active));
        when(userRoleRepository.findAllByUserId(active.id())).thenReturn(Flux.empty());

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
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        when(auditService.record(eq(actorId), eq("identity.user.status.change"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.changeStatus(actorId, userId, UserStatus.LOCKED))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.LOCKED))
            .verifyComplete();
        verify(auditService).record(actorId, "identity.user.status.change", "USER", userId, "{}");
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
        DurableEventService events = mock(DurableEventService.class);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.just(disabled));
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        when(events.append(eq("identity.user.disabled"), eq(1), eq("user"), eq(userId), any(String.class)))
            .thenReturn(Mono.empty());
        when(auditService.record(eq(actorId), eq("identity.user.status.change"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());
        DefaultUserService eventService = new DefaultUserService(userRepository, roleRepository, userRoleRepository,
            auditService, events);

        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.DISABLED))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.DISABLED))
            .verifyComplete();
        verify(events).append(eq("identity.user.disabled"), eq(1), eq("user"), eq(userId), any(String.class));
    }

    @Test
    void emitsCreatedAndEnabledLifecycleEvents() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        DurableEventService events = mock(DurableEventService.class);
        PlatformUserEntity created = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.PENDING, now, now, null, 0L);
        PlatformUserEntity disabled = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.DISABLED, now, now, null, 1L);
        PlatformUserEntity enabled = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 2L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(created), Mono.just(disabled));
        when(userRepository.save(any())).thenReturn(Mono.just(created), Mono.just(disabled), Mono.just(enabled));
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(Flux.empty());
        when(events.append(any(String.class), eq(1), eq("user"), eq(userId), any(String.class)))
            .thenReturn(Mono.empty());
        when(auditService.record(any(), any(String.class), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());
        DefaultUserService eventService = new DefaultUserService(userRepository, roleRepository, userRoleRepository,
            auditService, events);

        StepVerifier.create(eventService.create(actorId,
                new CreateUserRequest("alice", "Alice", "alice@example.com")))
            .assertNext(view -> assertThat(view.status()).isEqualTo(UserStatus.PENDING)).verifyComplete();
        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.DISABLED)).expectNextCount(1).verifyComplete();
        StepVerifier.create(eventService.changeStatus(actorId, userId, UserStatus.ACTIVE)).expectNextCount(1).verifyComplete();
        verify(events).append(eq("identity.user.created"), eq(1), eq("user"), eq(userId), any(String.class));
        verify(events).append(eq("identity.user.enabled"), eq(1), eq("user"), eq(userId), any(String.class));
    }

    @Test
    void assignsMissingRoleIdempotently() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "NORMAL_USER", "普通用户", null,
            false, now, now, 0L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId)).thenReturn(Mono.empty());
        when(userRoleRepository.save(any())).thenReturn(Mono.just(new UserRoleEntity(UUID.randomUUID(), userId, roleId,
            now, 0L)));
        when(auditService.record(eq(actorId), eq("identity.user.role.assign"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.assignRole(actorId, userId, roleId)).verifyComplete();
        verify(userRoleRepository).save(any(UserRoleEntity.class));
    }

    @Test
    void removesExistingRoleAndAuditsChange() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        PlatformRoleEntity role = new PlatformRoleEntity(roleId, "NORMAL_USER", "普通用户", null,
            false, now, now, 0L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(roleRepository.findById(roleId)).thenReturn(Mono.just(role));
        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId))
            .thenReturn(Mono.just(new UserRoleEntity(UUID.randomUUID(), userId, roleId, now, 0L)));
        when(userRoleRepository.deleteByUserIdAndRoleId(userId, roleId)).thenReturn(Mono.empty());
        when(auditService.record(eq(actorId), eq("identity.user.role.remove"), eq("USER"), eq(userId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.removeRole(actorId, userId, roleId)).verifyComplete();
        verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
    }
}
