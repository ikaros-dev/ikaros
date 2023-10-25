package run.ikaros.api.infra.exception;

import org.pf4j.PluginRuntimeException;

public class IkarosPluginException extends PluginRuntimeException {
    public IkarosPluginException() {
    }

    public IkarosPluginException(String message) {
        super(message);
    }

    public IkarosPluginException(Throwable cause) {
        super(cause);
    }

    public IkarosPluginException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public IkarosPluginException(String message, Object... args) {
        super(message, args);
    }
}
