package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class RegexMatchingException extends RuntimeIkarosException {
    public RegexMatchingException() {
    }

    public RegexMatchingException(String message) {
        super(message);
    }

    public RegexMatchingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegexMatchingException(Throwable cause) {
        super(cause);
    }

    public RegexMatchingException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
