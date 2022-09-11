package cn.liguohao.ikaros.common;

import cn.liguohao.ikaros.constants.SecurityConstants;
import cn.liguohao.ikaros.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtKit {
    private static final Logger logger = LoggerFactory.getLogger(JwtKit.class);

    private static final byte[] secretKey =
        DatatypeConverter.parseBase64Binary(SecurityConstants.JWT_SECRET_KEY);

    private JwtKit() {
    }


    public static String generateTokenByUsernameAndRoles(String userName, Long userId,
                                                         List<String> roles,
                                                         boolean isRemember) {
        byte[] jwtSecretKey = DatatypeConverter.parseBase64Binary(SecurityConstants.JWT_SECRET_KEY);
        long expiration = isRemember ? SecurityConstants.EXPIRATION_REMEMBER_TIME :
            SecurityConstants.EXPIRATION_TIME;
        String token = Jwts.builder()
            .setHeaderParam("type", SecurityConstants.TOKEN_TYPE)
            .setHeaderParam(SecurityConstants.HEADER_UID, userId)
            .signWith(Keys.hmacShaKeyFor(jwtSecretKey), SignatureAlgorithm.HS256)
            .setSubject(userName)
            .claim(SecurityConstants.TOKEN_ROLE_CLAIM, roles)
            .setIssuer(SecurityConstants.TOKEN_ISSUER)
            .setIssuedAt(new Date())
            .setAudience(SecurityConstants.TOKEN_AUDIENCE)
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
            (List<String>) claims.get(SecurityConstants.TOKEN_ROLE_CLAIM);
        List<SimpleGrantedAuthority> authorities =
            Objects.isNull(roles) ? Collections.singletonList(new SimpleGrantedAuthority(
                Role.MASTER.name())) :
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
        Assert.hasText(token, "'token' must has text");
        Assert.hasText(key, "'key' must has text");
        Jws<Claims> claimsJws = parseToken(token);
        if (claimsJws != null && claimsJws.getHeader() != null) {
            return claimsJws.getHeader().get(key);
        }
        return null;
    }

    public static Jws<Claims> parseToken(String token) {
        Assert.hasText(token, "'token' must has text");
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token);
    }

}
