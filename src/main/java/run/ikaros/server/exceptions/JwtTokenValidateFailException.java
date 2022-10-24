package run.ikaros.server.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * @author li-guohao
 */
public class JwtTokenValidateFailException extends AuthenticationException {
    public JwtTokenValidateFailException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JwtTokenValidateFailException(String msg) {
        super(msg);
    }
}
