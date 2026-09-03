package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;

/**
 * 默认安全会话服务，只保存安全状态，不保存任何可复用的令牌原文。
 */
@Service
public class DefaultSecuritySessionService implements SecuritySessionService {
    private final PlatformUserRepository userRepository;
    private final SecuritySessionRepository sessionRepository;
    private final AuditService auditService;
    private final DurableEventService eventService;

    /**
     * 创建安全会话服务。
     *
     * @param userRepository 用户仓储
     * @param sessionRepository 安全会话仓储
     * @param auditService 审计服务
     */
    public DefaultSecuritySessionService(PlatformUserRepository userRepository,
                                         SecuritySessionRepository sessionRepository,
                                         AuditService auditService) {
        this(userRepository, sessionRepository, auditService, null);
    }

    @Autowired
    public DefaultSecuritySessionService(PlatformUserRepository userRepository,
                                         SecuritySessionRepository sessionRepository,
                                         AuditService auditService, DurableEventService eventService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Override
    public Mono<SessionView> open(UUID userId, String loginMethod, Instant expiresAt) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            return Mono.error(new ConflictException("会话到期时间必须晚于当前时间"));
        }
        return activeUser(userId).flatMap(user -> sessionRepository.save(new SecuritySessionEntity(null, userId,
                user.securityVersion(), loginMethod, SecurityVerificationLevel.SVL_0.value(), null, null,
                expiresAt, null, now, now, null)))
            .map(this::toView);
    }

    @Override
    public Mono<SessionView> stepUp(UUID userId, UUID sessionId, SecurityVerificationLevel level,
                                    Instant verificationExpiresAt) {
        Instant now = Instant.now();
        if (!verificationExpiresAt.isAfter(now)) {
            return Mono.error(new ConflictException("验证保证到期时间必须晚于当前时间"));
        }
        return ownedActiveSession(userId, sessionId, now).flatMap(session -> {
            if (level.value() < session.currentSvl()) {
                return Mono.error(new ConflictException("不能降低会话的安全验证等级"));
            }
            SecuritySessionEntity steppedUp = new SecuritySessionEntity(session.id(), session.userId(),
                session.securityVersion(), session.loginMethod(), level.value(), now, verificationExpiresAt,
                session.expiresAt(), session.revokedAt(), now, session.createdAt(), session.version());
            return sessionRepository.save(steppedUp).map(this::toView);
        });
    }

    @Override
    public Flux<SessionView> listActive(UUID userId) {
        return sessionRepository.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, Instant.now())
            .map(this::toView);
    }

    @Override
    public Mono<Void> revoke(UUID actorId, UUID userId, UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .switchIfEmpty(Mono.error(new NotFoundException("会话不存在")))
            .flatMap(session -> {
                if (!session.userId().equals(userId)) {
                    return Mono.error(new NotFoundException("会话不存在"));
                }
                if (session.revokedAt() != null) {
                    return Mono.empty();
                }
                SecuritySessionEntity revoked = new SecuritySessionEntity(session.id(), session.userId(),
                    session.securityVersion(), session.loginMethod(), session.currentSvl(), session.verifiedAt(),
                    session.verificationExpiresAt(), session.expiresAt(), Instant.now(), session.lastActiveAt(),
                    session.createdAt(), session.version());
                return sessionRepository.save(revoked)
                    .flatMap(saved -> emitRevoked(saved).then());
            })
            .then(auditService.record(actorId, "identity.session.revoke", "SESSION", sessionId, "{}"));
    }

    @Override
    public Mono<Void> revokeAll(UUID actorId, UUID userId) {
        Instant now = Instant.now();
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在")))
            .flatMap(user -> {
                PlatformUserEntity changed = new PlatformUserEntity(user.id(), user.username(), user.displayName(),
                    user.email(), user.status(), user.createdAt(), now, user.lastLoginAt(),
                    user.securityVersion() + 1, user.version());
                return userRepository.save(changed)
                    .then(sessionRepository.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, now)
                        .flatMap(session -> sessionRepository.save(new SecuritySessionEntity(session.id(), session.userId(),
                            session.securityVersion(), session.loginMethod(), session.currentSvl(), session.verifiedAt(),
                            session.verificationExpiresAt(), session.expiresAt(), now, session.lastActiveAt(),
                            session.createdAt(), session.version())))
                        .then())
                    .then(emitAllRevoked(changed));
            })
            .then(auditService.record(actorId, "identity.session.revoke-all", "USER", userId, "{}"));
    }

    private Mono<Void> emitAllRevoked(PlatformUserEntity user) {
        if (eventService == null) return Mono.empty();
        return eventService.append("identity.user.sessions-revoked", 1, "user", user.id(),
            "{\"user_id\":\"" + user.id() + "\",\"security_version\":"
                + user.securityVersion() + "}").then();
    }

    private Mono<Void> emitRevoked(SecuritySessionEntity session) {
        if (eventService == null) return Mono.empty();
        return eventService.append("identity.session.revoked", 1, "session", session.id(),
            "{\"session_id\":\"" + session.id() + "\",\"user_id\":\"" + session.userId() + "\"}").then();
    }

    private Mono<PlatformUserEntity> activeUser(UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在")))
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .switchIfEmpty(Mono.error(new ConflictException("用户当前不可建立会话")));
    }

    private Mono<SecuritySessionEntity> ownedActiveSession(UUID userId, UUID sessionId, Instant now) {
        return sessionRepository.findById(sessionId)
            .filter(session -> session.userId().equals(userId) && session.revokedAt() == null
                && session.expiresAt().isAfter(now))
            .switchIfEmpty(Mono.error(new NotFoundException("活跃会话不存在")));
    }

    private SessionView toView(SecuritySessionEntity session) {
        return new SessionView(session.id(), session.userId(), session.loginMethod(),
            SecurityVerificationLevel.fromValue(session.currentSvl()), session.verifiedAt(),
            session.verificationExpiresAt(), session.expiresAt(), session.lastActiveAt());
    }
}
