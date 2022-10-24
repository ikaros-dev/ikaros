package run.ikaros.server.core.exceptions;

/**
 * @author li-guohao
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
