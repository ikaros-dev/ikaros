package run.ikaros.authentication;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.InitialRoleAssigner;
import run.ikaros.authorization.api.PermissionSnapshot;
import run.ikaros.authorization.api.PermissionSnapshotQuery;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class AuthenticationService {
    private final PlatformUserRepository users;
    private final PasswordCredentialRepository credentials;
    private final UserService userService;
    private final JwtTokenService tokens;
    private final InitialRoleAssigner initialRoleAssigner;
    private final PermissionSnapshotQuery permissionSnapshotQuery;
    private final TransactionalOperator transaction;

    public AuthenticationService(PlatformUserRepository users, PasswordCredentialRepository credentials,
                                  UserService userService, JwtTokenService tokens,
                                  InitialRoleAssigner initialRoleAssigner,
                                  PermissionSnapshotQuery permissionSnapshotQuery,
                                  TransactionalOperator transaction) {
        this.users = users;
        this.credentials = credentials;
        this.userService = userService;
        this.tokens = tokens;
        this.initialRoleAssigner = initialRoleAssigner;
        this.permissionSnapshotQuery = permissionSnapshotQuery;
        this.transaction = transaction;
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
                .then(assignAdminIfFirstUser(user)).thenReturn(user));
        return persisted.as(transaction::transactional)
            .flatMap(this::issue);
    }

    public Mono<AuthenticationView> login(LoginRequest request) {
        return users.findByUsername(request.username().trim())
            .flatMap(user -> credentials.findByUserId(user.id())
                .filter(credential -> PasswordHashService.matches(request.password(), credential.passwordHash()))
                .switchIfEmpty(Mono.error(new NotFoundException("用户名或密码错误")))
                .thenReturn(user))
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .switchIfEmpty(Mono.error(new NotFoundException("用户名或密码错误")))
            .flatMap(this::issue);
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
