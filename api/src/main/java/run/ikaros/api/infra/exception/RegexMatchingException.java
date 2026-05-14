package run.ikaros.api.infra.exception;

public class RegexMatchingException extends RuntimeException {
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
