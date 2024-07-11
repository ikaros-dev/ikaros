package run.ikaros.api.infra.exception.security;

import org.springframework.security.core.AuthenticationException;

public class UserAuthenticationException extends AuthenticationException {
    public UserAuthenticationException(String msg) {
        super(msg);
    }

    public UserAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
