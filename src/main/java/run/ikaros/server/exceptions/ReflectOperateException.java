package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class ReflectOperateException extends RuntimeIkarosException {
    public ReflectOperateException() {
    }

    public ReflectOperateException(String message) {
        super(message);
    }

    public ReflectOperateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReflectOperateException(Throwable cause) {
        super(cause);
    }

    public ReflectOperateException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
