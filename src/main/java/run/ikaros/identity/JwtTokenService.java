package run.ikaros.identity;

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

/** 签发并校验无状态 access token / refresh token。 */
@Service
public class JwtTokenService {
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final String SESSION_ID = "sid";
    private static final String PERMISSIONS = "permissions";

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

    public TokenPair issue(UUID userId, UUID sessionId, List<String> permissions) {
        Instant now = Instant.now();
        String accessToken = issue(userId, sessionId, permissions, ACCESS, now, accessTokenTtl);
        String refreshToken = issue(userId, sessionId, List.of(), REFRESH, now, refreshTokenTtl);
        return new TokenPair(accessToken, refreshToken, now.plus(accessTokenTtl));
    }

    public Claims verifyAccess(String token) {
        return claims(token, ACCESS);
    }

    public Claims verifyRefresh(String token) {
        return claims(token, REFRESH);
    }

    private String issue(UUID userId, UUID sessionId, List<String> permissions, String type,
                         Instant now, Duration ttl) {
        return JWT.create().withIssuer(issuer).withSubject(userId.toString())
            .withClaim(SESSION_ID, sessionId.toString())
            .withClaim(TOKEN_TYPE, type)
            .withClaim(PERMISSIONS, permissions)
            .withIssuedAt(Date.from(now)).withExpiresAt(Date.from(now.plus(ttl))).sign(algorithm);
    }

    private Claims claims(String token, String expectedType) {
        DecodedJWT jwt = verifier.verify(token);
        if (!expectedType.equals(jwt.getClaim(TOKEN_TYPE).asString())) {
            throw new IllegalArgumentException("JWT token 类型不正确");
        }
        return new Claims(UUID.fromString(jwt.getSubject()),
            UUID.fromString(jwt.getClaim(SESSION_ID).asString()),
            jwt.getClaim(PERMISSIONS).asList(String.class), jwt.getExpiresAt().toInstant());
    }

    public record TokenPair(String accessToken, String refreshToken, Instant accessTokenExpiresAt) { }

    public record Claims(UUID userId, UUID sessionId, List<String> permissions, Instant expiresAt) { }
}
