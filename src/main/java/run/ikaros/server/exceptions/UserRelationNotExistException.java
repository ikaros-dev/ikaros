package run.ikaros.server.exceptions;

/**
 * 查无此关系
 *
 * @author li-guohao
 * @date 2022/06/03
 */
public class UserRelationNotExistException extends RuntimeIkarosException {
    public UserRelationNotExistException() {
    }

    public UserRelationNotExistException(String message) {
        super(message);
    }

    public UserRelationNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserRelationNotExistException(Throwable cause) {
        super(cause);
    }

    public UserRelationNotExistException(String message, Throwable cause, boolean enableSuppression,
                                         boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
