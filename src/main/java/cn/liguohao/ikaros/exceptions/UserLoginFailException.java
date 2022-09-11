package cn.liguohao.ikaros.exceptions;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * @author li-guohao
 */
public class UserLoginFailException extends BadCredentialsException {
    public UserLoginFailException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserLoginFailException(String msg) {
        super(msg);
    }
}
