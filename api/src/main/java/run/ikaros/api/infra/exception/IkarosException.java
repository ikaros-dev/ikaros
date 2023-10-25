package run.ikaros.api.infra.exception;

public class IkarosException extends RuntimeException {
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
