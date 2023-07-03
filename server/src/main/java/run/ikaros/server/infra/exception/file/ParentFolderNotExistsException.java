package run.ikaros.server.infra.exception.file;

public class ParentFolderNotExistsException extends RuntimeException {
    public ParentFolderNotExistsException() {
    }

    public ParentFolderNotExistsException(String message) {
        super(message);
    }

    public ParentFolderNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParentFolderNotExistsException(Throwable cause) {
        super(cause);
    }

    public ParentFolderNotExistsException(String message, Throwable cause,
                                          boolean enableSuppression,
                                          boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
