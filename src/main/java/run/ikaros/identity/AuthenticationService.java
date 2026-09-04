package run.ikaros.identity;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class AuthenticationService {
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private final PlatformUserRepository users;
    private final PasswordCredentialRepository credentials;
    private final SecuritySessionService sessions;
    private final UserService userService;
    private final PlatformRoleRepository roles;
    private final UserRoleRepository userRoles;
    private final RolePermissionRepository rolePermissions;
    private final JwtTokenService tokens;
    private final SecureRandom random = new SecureRandom();

    public AuthenticationService(PlatformUserRepository users, PasswordCredentialRepository credentials,
                                  SecuritySessionService sessions, UserService userService,
                                  PlatformRoleRepository roles, UserRoleRepository userRoles,
                                  RolePermissionRepository rolePermissions, JwtTokenService tokens) {
        this.users = users;
        this.credentials = credentials;
        this.sessions = sessions;
        this.userService = userService;
        this.roles = roles;
        this.userRoles = userRoles;
        this.rolePermissions = rolePermissions;
        this.tokens = tokens;
    }

    public Mono<AuthenticationView> register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim().toLowerCase();
        Instant now = Instant.now();
        return users.save(new PlatformUserEntity(null, username, request.displayName().trim(), email,
                UserStatus.ACTIVE, now, now, null, 0L, null))
            .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("用户名或邮箱已存在"))
            .flatMap(user -> credentials.save(new PasswordCredentialEntity(null, user.id(), hash(request.password()), now, now, null))
                .then(assignAdminIfFirstUser(user)).thenReturn(user))
            .flatMap(this::issue);
    }

    public Mono<AuthenticationView> login(LoginRequest request) {
        return users.findByUsername(request.username().trim())
            .flatMap(user -> credentials.findByUserId(user.id())
                .filter(credential -> matches(request.password(), credential.passwordHash()))
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
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
                .flatMap(user -> issue(user, claims.sessionId()));
        } catch (RuntimeException invalidToken) {
            return Mono.error(new NotFoundException("刷新令牌无效或已过期"));
        }
    }

    public Mono<Void> logout() {
        return Mono.empty();
    }

    private Mono<Void> assignAdminIfFirstUser(PlatformUserEntity user) {
        return users.count().flatMap(count -> count == 1
            ? roles.findByCode("admin").switchIfEmpty(Mono.error(new IllegalStateException("内置 admin 角色未初始化")))
                .flatMap(role -> userRoles.save(new UserRoleEntity(null, user.id(), role.id(), Instant.now(), null))).then()
            : Mono.empty());
    }

    private Mono<AuthenticationView> issue(PlatformUserEntity user) {
        return sessions.open(user.id(), "PASSWORD", Instant.now().plusSeconds(30L * 24 * 3600))
            .flatMap(session -> issue(user, session.id()));
    }

    private Mono<AuthenticationView> issue(PlatformUserEntity user, UUID sessionId) {
        return userService.get(user.id()).zipWith(userPermissions(user.id()))
            .map(data -> {
                JwtTokenService.TokenPair pair = tokens.issue(user.id(), sessionId, data.getT2());
                return new AuthenticationView(user.id(), sessionId, pair.accessToken(), pair.refreshToken(),
                    pair.accessTokenExpiresAt(), data.getT1(), data.getT2());
            });
    }

    private Mono<java.util.List<String>> userPermissions(UUID userId) {
        return userRoles.findAllByUserId(userId)
            .flatMap(binding -> rolePermissions.findAllByRoleId(binding.roleId()))
            .map(RolePermissionEntity::permissionKey).distinct().sort().collectList();
    }

    private String hash(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        try {
            return "pbkdf2-sha256$" + ITERATIONS + "$" + b64(salt) + "$" + b64(derive(password.toCharArray(), salt));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split("\\$", 4);
            return parts.length == 4 && MessageDigest.isEqual(derive(password.toCharArray(),
                Base64.getUrlDecoder().decode(parts[2])), Base64.getUrlDecoder().decode(parts[3]));
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt) throws Exception {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)).getEncoded();
    }

    private String b64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
