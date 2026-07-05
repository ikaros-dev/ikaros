package run.ikaros.server.security.authentication.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.security.InvalidTokenException;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.security.SecurityProperties;

@Slf4j
@Component
public class JwtAuthenticationProvider {
    private final SecurityProperties securityProperties;
    private final String secretKey;
    private Long accessTokenExpiry = 0L;
    private Long refreshTokenExpiry = 0L;

    /**
     * Construct instance.
     */
    public JwtAuthenticationProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        secretKey = Base64.getEncoder().encodeToString(
            StringUtils.generateRandomStr(512).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * generateToken and convert to {@link JwtApplyResponse}.
     */
    public Mono<JwtApplyResponse> generateJwtResp(UserDetails userDetails) {
        String username = userDetails.getUsername();
        SecurityProperties.Expiry expiry = securityProperties.getExpiry();
        Integer accessTokenDay = expiry.getAccessTokenDay();
        Integer refreshTokenMonth = expiry.getRefreshTokenMonth();
        int dayOfMs = 24 * 60 * 60 * 1000;
        accessTokenExpiry = (long) (accessTokenDay * dayOfMs);
        refreshTokenExpiry = (long) refreshTokenMonth * dayOfMs * 30;
        String accessToken = generateToken(username, accessTokenExpiry);
        String refreshToken = generateToken(username, refreshTokenExpiry);
        return Mono.just(JwtApplyResponse.builder()
            .username(username)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build());
    }

    /**
     * 生成临时令牌（短时效，用于二步验证流程）.
     */
    public String generateTempToken(String username) {
        long tempTokenExpiry = 5 * 60 * 1000L;
        return generateToken(username, tempTokenExpiry);
    }

    /**
     * validateToken.
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 提取token中的用户名.
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * 验证tempToken并提取用户名.
     */
    public String tempTokenUsername(String tempToken) {
        if (!validateToken(tempToken)) {
            throw new InvalidTokenException("Invalid or expired temp token.");
        }
        return extractUsername(tempToken);
    }

    /**
     * 刷新token.
     *
     * @return 新的accessToken
     */
    public Mono<String> refreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            return Mono.error(new InvalidTokenException("Invalid token"));
        }
        return Mono.just(generateToken(extractUsername(refreshToken), accessTokenExpiry));
    }

    /**
     * generateToken.
     */
    private String generateToken(String username, long milliseconds) {
        return Jwts.builder()
            .setSubject(username)
            .setExpiration(new Date(System.currentTimeMillis() + milliseconds))
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }

    /**
     * extractClaims.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
