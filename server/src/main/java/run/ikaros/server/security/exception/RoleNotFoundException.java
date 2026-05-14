package run.ikaros.server.security.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;

public class RoleNotFoundException extends AuthenticationException {
    public RoleNotFoundException(@Nullable String msg, Throwable cause) {
        super(msg, cause);
    }

    public RoleNotFoundException(@Nullable String msg) {
        super(msg);
    }
}
