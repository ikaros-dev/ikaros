package run.ikaros.server.security.authentication.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.SecurityProperties;

@Slf4j
@Component
public class JwtAuthenticationProvider {
    private final SecurityProperties securityProperties;
    private final String secretKey;
    private final long expirationTime; // 1 d

    /**
     * Construct instance.
     */
    public JwtAuthenticationProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        secretKey = Base64.getEncoder().encodeToString(
            securityProperties.getJwtSecretKey().getBytes(StandardCharsets.UTF_8));
        expirationTime = securityProperties.getJwtExpirationTime();
    }

    /**
     * generateToken.
     */
    public String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
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
