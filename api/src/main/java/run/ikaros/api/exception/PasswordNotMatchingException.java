package run.ikaros.api.exception;

import org.springframework.security.core.AuthenticationException;

public class PasswordNotMatchingException extends AuthenticationException {
    public PasswordNotMatchingException(String msg) {
        super(msg);
    }

    public PasswordNotMatchingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
