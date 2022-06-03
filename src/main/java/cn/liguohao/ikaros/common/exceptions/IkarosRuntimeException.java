package cn.liguohao.ikaros.common.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
public class IkarosRuntimeException extends RuntimeException {
    public IkarosRuntimeException() {
    }

    public IkarosRuntimeException(String message) {
        super(message);
    }

    public IkarosRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IkarosRuntimeException(Throwable cause) {
        super(cause);
    }

    public IkarosRuntimeException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
