package run.ikaros.server.security;

import org.springframework.security.core.AuthenticationException;

/**
 * jwt authentication exception.
 *
 * @author: li-guohao
 */
public class JwtAuthenticationException extends AuthenticationException {
    public JwtAuthenticationException(String msg) {
        super(msg);
    }
}
