package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class MikanRequestException extends RuntimeIkarosException {
    public MikanRequestException() {
    }

    public MikanRequestException(String message) {
        super(message);
    }

    public MikanRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MikanRequestException(Throwable cause) {
        super(cause);
    }

    public MikanRequestException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
