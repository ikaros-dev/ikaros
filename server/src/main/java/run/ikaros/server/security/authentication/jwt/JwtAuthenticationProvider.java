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
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.security.SecurityProperties;

@Slf4j
@Component
public class JwtAuthenticationProvider {
    private final SecurityProperties securityProperties;
    private final String secretKey;

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
        String accessToken = generateToken(username, accessTokenDay * dayOfMs);
        String refreshToken = generateToken(username, (long) refreshTokenMonth * dayOfMs * 30);
        return Mono.just(JwtApplyResponse.builder()
            .username(username)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build());
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

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
}
