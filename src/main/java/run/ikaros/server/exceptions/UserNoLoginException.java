package run.ikaros.server.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * @author li-guohao
 */
public class UserNoLoginException extends AuthenticationException {
    public UserNoLoginException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserNoLoginException(String msg) {
        super(msg);
    }
}
