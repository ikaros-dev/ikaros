package run.ikaros.server.security.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

public class UserHasNotRoleException extends AuthenticationException {
    public UserHasNotRoleException(@Nullable String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserHasNotRoleException(@Nullable String msg) {
        super(msg);
    }
}
