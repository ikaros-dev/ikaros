package run.ikaros.authentication;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.InitialRoleAssigner;
import run.ikaros.authorization.api.PermissionSnapshot;
import run.ikaros.authorization.api.PermissionSnapshotQuery;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.operations.api.AuditService;

@Service
public class AuthenticationService {
    private final PlatformUserRepository users;
    private final PasswordCredentialRepository credentials;
    private final UserService userService;
    private final JwtTokenService tokens;
    private final InitialRoleAssigner initialRoleAssigner;
    private final PermissionSnapshotQuery permissionSnapshotQuery;
    private final TransactionalOperator transaction;
    private final AuditService auditService;

    public AuthenticationService(PlatformUserRepository users, PasswordCredentialRepository credentials,
                                  UserService userService, JwtTokenService tokens,
                                  InitialRoleAssigner initialRoleAssigner,
                                  PermissionSnapshotQuery permissionSnapshotQuery,
                                  TransactionalOperator transaction) {
        this(users, credentials, userService, tokens, initialRoleAssigner, permissionSnapshotQuery, transaction, null);
    }

    @Autowired
    public AuthenticationService(PlatformUserRepository users, PasswordCredentialRepository credentials,
                                  UserService userService, JwtTokenService tokens,
                                  InitialRoleAssigner initialRoleAssigner,
                                  PermissionSnapshotQuery permissionSnapshotQuery,
                                  TransactionalOperator transaction, AuditService auditService) {
        this.users = users;
        this.credentials = credentials;
        this.userService = userService;
        this.tokens = tokens;
        this.initialRoleAssigner = initialRoleAssigner;
        this.permissionSnapshotQuery = permissionSnapshotQuery;
        this.transaction = transaction;
        this.auditService = auditService;
    }

    public Mono<AuthenticationView> register(RegisterRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
            || request.password() == null || request.password().length() < 8
            || request.displayName() == null || request.displayName().isBlank()) {
            return Mono.error(new IllegalArgumentException("注册信息不合法"));
        }
        String username = request.username().trim();
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim().toLowerCase();
        Instant now = Instant.now();
        Mono<PlatformUserEntity> persisted = users.save(new PlatformUserEntity(null, username, request.displayName().trim(), email,
                UserStatus.ACTIVE, now, now, null, 0L, null))
            .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("用户名或邮箱已存在"))
                .flatMap(user -> credentials.save(new PasswordCredentialEntity(null, user.id(),
                    PasswordHashService.hash(request.password()), now, now, null))
                .then(assignAdminIfFirstUser(user))
                .then(record(userAudit(user.id(), "authentication.register", "USER", user.id(),
                    AuditResult.SUCCESS, AuditRiskLevel.SENSITIVE, subjectDetails(username))))
                .thenReturn(user));
        return persisted.as(transaction::transactional)
            .flatMap(this::issue);
    }

    public Mono<AuthenticationView> login(LoginRequest request) {
        String subjectHint = request.username().trim();
        return users.findByUsername(subjectHint)
            .flatMap(user -> credentials.findByUserId(user.id())
                .filter(credential -> PasswordHashService.matches(request.password(), credential.passwordHash()))
                .map(credential -> user))
            .flatMap(user -> user.status() == UserStatus.ACTIVE
                ? issue(user).flatMap(view -> record(userAudit(user.id(), "authentication.login", "USER", user.id(),
                    AuditResult.SUCCESS, AuditRiskLevel.SENSITIVE, subjectDetails(subjectHint))).thenReturn(view))
                : deniedLogin(user.id(), subjectHint))
            .switchIfEmpty(failedLogin(subjectHint));
    }

    public Mono<AuthenticationView> refresh(String refreshToken) {
        try {
            JwtTokenService.Claims claims = tokens.verifyRefresh(refreshToken);
            return users.findById(claims.userId())
                .filter(user -> user.status() == UserStatus.ACTIVE
                    && user.securityVersion() == claims.securityVersion())
                .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
                .flatMap(this::issue);
        } catch (RuntimeException invalidToken) {
            return Mono.error(new NotFoundException("刷新令牌无效或已过期"));
        }
    }

    public Mono<Void> logout() {
        return Mono.empty();
    }

    private Mono<AuthenticationView> failedLogin(String subjectHint) {
        return record(new AuditEventCommand(AuditActorType.ANONYMOUS, null, "authentication.login", "AUTHENTICATION",
            null, AuditResult.FAILURE, AuditRiskLevel.SENSITIVE, subjectDetails(subjectHint), 1, null))
            .then(Mono.error(new NotFoundException("用户名或密码错误")));
    }

    private Mono<AuthenticationView> deniedLogin(UUID userId, String subjectHint) {
        return record(new AuditEventCommand(AuditActorType.ANONYMOUS, null, "authentication.login", "USER", userId,
            AuditResult.DENIED, AuditRiskLevel.SENSITIVE, subjectDetails(subjectHint), 1, null))
            .then(Mono.error(new NotFoundException("用户名或密码错误")));
    }

    private AuditEventCommand userAudit(UUID actorId, String action, String targetType, UUID targetId,
                                        AuditResult result, AuditRiskLevel riskLevel, String details) {
        return new AuditEventCommand(AuditActorType.USER, actorId, action, targetType, targetId,
            result, riskLevel, details, 1, null);
    }

    private Mono<Void> record(AuditEventCommand command) {
        return auditService == null ? Mono.empty() : auditService.record(command);
    }

    private String subjectDetails(String subjectHint) {
        return "{\"subject_hint\":\"" + escapeJson(subjectHint) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Mono<Void> assignAdminIfFirstUser(PlatformUserEntity user) {
        return users.count().flatMap(count -> count == 1
            ? initialRoleAssigner.assignInitialRole(user.id(), "admin")
            : Mono.empty());
    }

    private Mono<AuthenticationView> issue(PlatformUserEntity user) {
        return userService.get(user.id()).zipWith(permissionSnapshotQuery.permissionsFor(user.id())
            .map(PermissionSnapshot::permissionKeys))
            .map(data -> {
                JwtTokenService.TokenPair pair = tokens.issue(user.id(), user.securityVersion(), data.getT2());
                return new AuthenticationView(user.id(), pair.accessToken(), pair.refreshToken(),
                    pair.accessTokenExpiresAt(), data.getT1(), data.getT2());
            });
    }

}
