package cn.liguohao.ikaros.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class NotSupportException extends IkarosException {
    public NotSupportException() {
    }

    public NotSupportException(String message) {
        super(message);
    }

    public NotSupportException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportException(Throwable cause) {
        super(cause);
    }

    public NotSupportException(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
