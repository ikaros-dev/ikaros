package run.ikaros.api.infra.exception;

public class FeatureNotImplException extends RuntimeException {

    public FeatureNotImplException(String message) {
        super(message);
    }

    public FeatureNotImplException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeatureNotImplException(Throwable cause) {
        super(cause);
    }

    public FeatureNotImplException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public FeatureNotImplException() {
        super("Feature not implemented yet!");
    }
}
