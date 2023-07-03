package run.ikaros.server.infra.exception.file;

public class FolderExistsException extends RuntimeException {
    public FolderExistsException() {
    }

    public FolderExistsException(String message) {
        super(message);
    }

    public FolderExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public FolderExistsException(Throwable cause) {
        super(cause);
    }

    public FolderExistsException(String message, Throwable cause, boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
