package run.ikaros.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import run.ikaros.authentication.verification.VerificationPurpose;

/** 签发并校验无状态 access token / refresh token。 */
@Service
public class JwtTokenService {
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final String VERIFICATION = "verification";
    private static final String SECURITY_VERSION = "security_version";
    private static final String PERMISSIONS = "permissions";
    private static final String PURPOSE = "purpose";
    private static final String TARGET_REFERENCE = "target_reference";
    private static final String ACHIEVED_SVL = "achieved_svl";
    private static final String VERIFIED_AT = "verified_at";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenService(
        @Value("${ikaros.security.jwt.issuer}") String issuer,
        @Value("${ikaros.security.jwt.secret}") String secret,
        @Value("${ikaros.security.jwt.access-token-ttl}") Duration accessTokenTtl,
        @Value("${ikaros.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("IKAROS_JWT_SECRET 至少需要 32 个字符");
        }
        this.issuer = issuer;
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public TokenPair issue(UUID userId, long securityVersion, List<String> permissions) {
        Instant now = Instant.now();
        String accessToken = issue(userId, securityVersion, permissions, ACCESS, now, accessTokenTtl);
        String refreshToken = issue(userId, securityVersion, List.of(), REFRESH, now, refreshTokenTtl);
        return new TokenPair(accessToken, refreshToken, now.plus(accessTokenTtl));
    }

    public Claims verifyAccess(String token) {
        return claims(token, ACCESS);
    }

    public Claims verifyRefresh(String token) {
        return claims(token, REFRESH);
    }

    public String issueVerificationGrant(UUID userId, long securityVersion, VerificationPurpose purpose,
                                         String targetReference, int achievedSvl,
                                         Instant verifiedAt, Instant expiresAt) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("验证凭据到期时间必须晚于当前时间");
        }
        return JWT.create().withIssuer(issuer).withJWTId(UUID.randomUUID().toString())
            .withSubject(userId.toString()).withClaim(TOKEN_TYPE, VERIFICATION)
            .withClaim(SECURITY_VERSION, securityVersion).withClaim(PURPOSE, purpose.name())
            .withClaim(TARGET_REFERENCE, targetReference).withClaim(ACHIEVED_SVL, achievedSvl)
            .withClaim(VERIFIED_AT, Date.from(verifiedAt)).withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt)).sign(algorithm);
    }

    public VerificationGrantClaims verifyVerificationGrant(String token) {
        DecodedJWT jwt = verifier.verify(token);
        if (!VERIFICATION.equals(jwt.getClaim(TOKEN_TYPE).asString())) {
            throw new IllegalArgumentException("JWT token 类型不正确");
        }
        return new VerificationGrantClaims(UUID.fromString(jwt.getSubject()), tokenId(jwt),
            requiredLong(jwt, SECURITY_VERSION), VerificationPurpose.valueOf(jwt.getClaim(PURPOSE).asString()),
            jwt.getClaim(TARGET_REFERENCE).isNull() ? null : jwt.getClaim(TARGET_REFERENCE).asString(),
            jwt.getClaim(ACHIEVED_SVL).asInt(), jwt.getClaim(VERIFIED_AT).asDate().toInstant(),
            jwt.getExpiresAt().toInstant());
    }

    private String issue(UUID userId, long securityVersion, List<String> permissions, String type,
                         Instant now, Duration ttl) {
        return JWT.create().withIssuer(issuer).withJWTId(UUID.randomUUID().toString())
            .withSubject(userId.toString()).withClaim(SECURITY_VERSION, securityVersion)
            .withClaim(TOKEN_TYPE, type)
            .withClaim(PERMISSIONS, permissions)
            .withIssuedAt(Date.from(now)).withExpiresAt(Date.from(now.plus(ttl))).sign(algorithm);
    }

    private Claims claims(String token, String expectedType) {
        DecodedJWT jwt = verifier.verify(token);
        if (!expectedType.equals(jwt.getClaim(TOKEN_TYPE).asString())) {
            throw new IllegalArgumentException("JWT token 类型不正确");
        }
        return new Claims(UUID.fromString(jwt.getSubject()), tokenId(jwt), requiredLong(jwt, SECURITY_VERSION),
            jwt.getClaim(PERMISSIONS).isNull() ? List.of() : jwt.getClaim(PERMISSIONS).asList(String.class),
            jwt.getExpiresAt().toInstant());
    }

    private UUID tokenId(DecodedJWT jwt) {
        String id = jwt.getId();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("JWT 缺少 jti");
        return UUID.fromString(id);
    }

    private long requiredLong(DecodedJWT jwt, String claim) {
        Long value = jwt.getClaim(claim).asLong();
        if (value == null) throw new IllegalArgumentException("JWT 缺少 " + claim);
        return value;
    }

    public record TokenPair(String accessToken, String refreshToken, Instant accessTokenExpiresAt) { }

    public record Claims(UUID userId, UUID tokenId, long securityVersion, List<String> permissions,
                         Instant expiresAt) { }

    public record VerificationGrantClaims(UUID userId, UUID grantId, long securityVersion,
                                          VerificationPurpose purpose, String targetReference,
                                          int achievedSvl, Instant verifiedAt, Instant expiresAt) { }
}
