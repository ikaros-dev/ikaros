package run.ikaros.api.exception;

import org.pf4j.PluginRuntimeException;

public class PluginInstallRuntimeException extends PluginRuntimeException {
    public PluginInstallRuntimeException() {
    }

    public PluginInstallRuntimeException(String message) {
        super(message);
    }

    public PluginInstallRuntimeException(Throwable cause) {
        super(cause);
    }

    public PluginInstallRuntimeException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public PluginInstallRuntimeException(String message, Object... args) {
        super(message, args);
    }
}
