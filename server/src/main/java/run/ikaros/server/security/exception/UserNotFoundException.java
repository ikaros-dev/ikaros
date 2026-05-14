package run.ikaros.server.security.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

public class UserNotFoundException extends AuthenticationException {
    public UserNotFoundException(@Nullable String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserNotFoundException(@Nullable String msg) {
        super(msg);
    }
}
