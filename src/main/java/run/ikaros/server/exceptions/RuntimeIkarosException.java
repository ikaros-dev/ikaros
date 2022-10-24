package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
public class RuntimeIkarosException extends RuntimeException {
    public RuntimeIkarosException() {
    }

    public RuntimeIkarosException(String message) {
        super(message);
    }

    public RuntimeIkarosException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeIkarosException(Throwable cause) {
        super(cause);
    }

    public RuntimeIkarosException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
