package run.ikaros.server.exceptions;

/**
 * @author li-guohao
 */
public class PresetOptionIllegalException extends RuntimeIkarosException {
    public PresetOptionIllegalException() {
    }

    public PresetOptionIllegalException(String message) {
        super(message);
    }

    public PresetOptionIllegalException(String message, Throwable cause) {
        super(message, cause);
    }

    public PresetOptionIllegalException(Throwable cause) {
        super(cause);
    }

    public PresetOptionIllegalException(String message, Throwable cause, boolean enableSuppression,
                                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
