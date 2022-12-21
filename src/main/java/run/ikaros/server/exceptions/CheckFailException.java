package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class CheckFailException extends IkarosException {
    public CheckFailException() {
    }

    public CheckFailException(String message) {
        super(message);
    }

    public CheckFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckFailException(Throwable cause) {
        super(cause);
    }

    public CheckFailException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
