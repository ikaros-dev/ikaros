package cn.liguohao.ikaros.common.exceptions.security;

import org.springframework.security.core.AuthenticationException;

/**
 * @author li-guohao
 */
public class UserHasExistException extends AuthenticationException {
    public UserHasExistException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserHasExistException(String msg) {
        super(msg);
    }
}
