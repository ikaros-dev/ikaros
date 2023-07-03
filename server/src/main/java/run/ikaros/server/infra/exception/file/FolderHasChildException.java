package run.ikaros.server.infra.exception.file;

public class FolderHasChildException extends RuntimeException {
    public FolderHasChildException() {
    }

    public FolderHasChildException(String message) {
        super(message);
    }

    public FolderHasChildException(String message, Throwable cause) {
        super(message, cause);
    }

    public FolderHasChildException(Throwable cause) {
        super(cause);
    }

    public FolderHasChildException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
