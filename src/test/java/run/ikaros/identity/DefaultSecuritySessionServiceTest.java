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

/** 验证安全会话的创建、升级、查询和撤销规则。 */
class DefaultSecuritySessionServiceTest {
    private PlatformUserRepository userRepository;
    private SecuritySessionRepository sessionRepository;
    private AuditService auditService;
    private DefaultSecuritySessionService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        sessionRepository = mock(SecuritySessionRepository.class);
        auditService = mock(AuditService.class);
        service = new DefaultSecuritySessionService(userRepository, sessionRepository, auditService);
    }

    @Test
    void opensSvlZeroSessionOnlyForActiveUser() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 0L);
        SecuritySessionEntity saved = new SecuritySessionEntity(sessionId, userId, "EMAIL_OTP", 0, null, null,
            now.plusSeconds(3600), null, now, now, 0L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(sessionRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.open(userId, "EMAIL_OTP", now.plusSeconds(3600)))
            .assertNext(view -> assertThat(view.currentSvl()).isEqualTo(SecurityVerificationLevel.SVL_0))
            .verifyComplete();
    }

    @Test
    void stepUpUpdatesSessionVerificationWindow() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        SecuritySessionEntity session = new SecuritySessionEntity(sessionId, userId, "PASSWORD", 0, null, null,
            now.plusSeconds(3600), null, now, now, 0L);
        SecuritySessionEntity verified = new SecuritySessionEntity(sessionId, userId, "PASSWORD", 1, now,
            now.plusSeconds(300), now.plusSeconds(3600), null, now, now, 1L);
        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(sessionRepository.save(any())).thenReturn(Mono.just(verified));

        StepVerifier.create(service.stepUp(userId, sessionId, SecurityVerificationLevel.SVL_1, now.plusSeconds(300)))
            .assertNext(view -> assertThat(view.currentSvl()).isEqualTo(SecurityVerificationLevel.SVL_1))
            .verifyComplete();
    }

    @Test
    void listsOnlyActiveSessionsForUser() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        SecuritySessionEntity session = new SecuritySessionEntity(UUID.randomUUID(), userId, "EMAIL_OTP", 1, now,
            now.plusSeconds(300), now.plusSeconds(3600), null, now, now, 0L);
        when(sessionRepository.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(eq(userId), any()))
            .thenReturn(Flux.just(session));

        StepVerifier.create(service.listActive(userId))
            .assertNext(view -> assertThat(view.id()).isEqualTo(session.id()))
            .verifyComplete();
    }

    @Test
    void revokesSessionAndWritesAuditEvent() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        SecuritySessionEntity session = new SecuritySessionEntity(sessionId, userId, "EMAIL_OTP", 0, null, null,
            now.plusSeconds(3600), null, now, now, 0L);
        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(sessionRepository.save(any())).thenReturn(Mono.just(session));
        when(auditService.record(eq(actorId), eq("identity.session.revoke"), eq("SESSION"), eq(sessionId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.revoke(actorId, userId, sessionId)).verifyComplete();
        verify(sessionRepository).save(any(SecuritySessionEntity.class));
    }
}
