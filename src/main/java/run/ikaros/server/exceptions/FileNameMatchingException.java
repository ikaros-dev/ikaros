package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class FileNameMatchingException extends RuntimeIkarosException {
    public FileNameMatchingException() {
    }

    public FileNameMatchingException(String message) {
        super(message);
    }

    public FileNameMatchingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNameMatchingException(Throwable cause) {
        super(cause);
    }

    public FileNameMatchingException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
