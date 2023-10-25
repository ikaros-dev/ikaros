package run.ikaros.api.infra.exception.security;

import run.ikaros.api.infra.exception.IkarosAuthenticationException;

public class PasswordNotMatchingException extends IkarosAuthenticationException {
    public PasswordNotMatchingException(String msg) {
        super(msg);
    }

    public PasswordNotMatchingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
