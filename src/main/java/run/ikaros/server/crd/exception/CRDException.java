package run.ikaros.server.crd.exception;

/**
 * @author: li-guohao
 */
public class CRDException extends RuntimeException {
    public CRDException() {
    }

    public CRDException(String message) {
        super(message);
    }

    public CRDException(String message, Throwable cause) {
        super(message, cause);
    }

    public CRDException(Throwable cause) {
        super(cause);
    }

    public CRDException(String message, Throwable cause, boolean enableSuppression,
                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
