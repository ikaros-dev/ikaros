package run.ikaros.server.core.exceptions;

/**
 * @author guohao
 */
public class IllegalArgumentRuntimeIkarosException extends RuntimeIkarosException {
    public IllegalArgumentRuntimeIkarosException() {
    }

    public IllegalArgumentRuntimeIkarosException(String message) {
        super(message);
    }

    public IllegalArgumentRuntimeIkarosException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentRuntimeIkarosException(Throwable cause) {
        super(cause);
    }

    public IllegalArgumentRuntimeIkarosException(String message, Throwable cause,
                                                 boolean enableSuppression,
                                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
