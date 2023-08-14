package run.ikaros.api.infra.exception.file;

public class FileExistsException extends RuntimeException {
    public FileExistsException() {
    }

    public FileExistsException(String message) {
        super(message);
    }

    public FileExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileExistsException(Throwable cause) {
        super(cause);
    }

    public FileExistsException(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
