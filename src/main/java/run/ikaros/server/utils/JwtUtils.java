package run.ikaros.server.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.constants.UserConst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private static final byte[] secretKey =
        DatatypeConverter.parseBase64Binary(SecurityConst.JWT_SECRET_KEY);

    private JwtUtils() {
    }


    public static String generateTokenBySubjectAndRoles(String subject, Long userId,
                                                        boolean isRemember) {
        List<String> roles = new ArrayList<>();
        roles.add(UserConst.DEFAULT_ROLE);

        byte[] jwtSecretKey = DatatypeConverter.parseBase64Binary(SecurityConst.JWT_SECRET_KEY);
        long expiration = isRemember ? SecurityConst.EXPIRATION_REMEMBER_TIME :
            SecurityConst.EXPIRATION_TIME;
        String token = Jwts.builder()
            .setHeaderParam("type", SecurityConst.TOKEN_TYPE)
            .setHeaderParam(SecurityConst.HEADER_UID, userId)
            .signWith(Keys.hmacShaKeyFor(jwtSecretKey), SignatureAlgorithm.HS256)
            .setSubject(subject)
            .claim(SecurityConst.TOKEN_ROLE_CLAIM, roles)
            .setIssuer(SecurityConst.TOKEN_ISSUER)
            .setIssuedAt(new Date())
            .setAudience(SecurityConst.TOKEN_AUDIENCE)
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .compact();
        return token;
    }

    public static boolean validateToken(String token) {
        try {
            getTokenBody(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("Request to parse expired JWT : {} failed : {}", token, e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("Request to parse unsupported JWT : {} failed : {}", token, e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("Request to parse invalid JWT : {} failed : {}", token, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Request to parse empty or null JWT : {} failed : {}", token,
                e.getMessage());
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public static Authentication getAuthentication(String token) {
        Claims claims = getTokenBody(token);
        List<String> roles =
            (List<String>) claims.get(SecurityConst.TOKEN_ROLE_CLAIM);
        List<SimpleGrantedAuthority> authorities =
            Objects.isNull(roles) ? Collections.singletonList(new SimpleGrantedAuthority(
                UserConst.DEFAULT_ROLE)) :
                roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        String userName = claims.getSubject();

        return new UsernamePasswordAuthenticationToken(userName, token, authorities);

    }

    private static Claims getTokenBody(String token) {
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();
    }

    public static Object getTokenHeaderValue(String token, String key) {
        AssertUtils.notBlank(token, "token");
        AssertUtils.notBlank(key, "key");
        Jws<Claims> claimsJws = parseToken(token);
        if (claimsJws != null && claimsJws.getHeader() != null) {
            return claimsJws.getHeader().get(key);
        }
        return null;
    }

    public static Jws<Claims> parseToken(String token) {
        AssertUtils.notBlank(token, "token");
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token);
    }

}
