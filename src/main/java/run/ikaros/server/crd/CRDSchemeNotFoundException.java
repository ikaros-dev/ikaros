package run.ikaros.server.crd;

/**
 * @author: li-guohao
 */
public class CRDSchemeNotFoundException extends RuntimeException {
    public CRDSchemeNotFoundException() {
    }

    public CRDSchemeNotFoundException(String message) {
        super(message);
    }

    public CRDSchemeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CRDSchemeNotFoundException(Throwable cause) {
        super(cause);
    }

    public CRDSchemeNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
