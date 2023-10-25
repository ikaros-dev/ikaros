package run.ikaros.api.infra.exception;

import org.springframework.security.core.AuthenticationException;

public class IkarosAuthenticationException extends AuthenticationException {
    public IkarosAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public IkarosAuthenticationException(String msg) {
        super(msg);
    }
}
