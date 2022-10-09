package cn.liguohao.ikaros.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class NotSupportRuntimeException extends IkarosRuntimeException {
    public NotSupportRuntimeException() {
    }

    public NotSupportRuntimeException(String message) {
        super(message);
    }

    public NotSupportRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportRuntimeException(Throwable cause) {
        super(cause);
    }

    public NotSupportRuntimeException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
