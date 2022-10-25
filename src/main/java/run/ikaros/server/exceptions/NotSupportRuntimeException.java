package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class NotSupportRuntimeException extends RuntimeIkarosException {
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
