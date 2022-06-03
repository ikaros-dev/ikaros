package cn.liguohao.ikaros.common.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
public class IkarosException extends Exception {
    public IkarosException() {
    }

    public IkarosException(String message) {
        super(message);
    }

    public IkarosException(String message, Throwable cause) {
        super(message, cause);
    }

    public IkarosException(Throwable cause) {
        super(cause);
    }

    public IkarosException(String message, Throwable cause, boolean enableSuppression,
                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
