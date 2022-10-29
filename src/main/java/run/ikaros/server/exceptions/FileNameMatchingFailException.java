package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class FileNameMatchingFailException extends RuntimeIkarosException {
    public FileNameMatchingFailException() {
    }

    public FileNameMatchingFailException(String message) {
        super(message);
    }

    public FileNameMatchingFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNameMatchingFailException(Throwable cause) {
        super(cause);
    }

    public FileNameMatchingFailException(String message, Throwable cause, boolean enableSuppression,
                                         boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
