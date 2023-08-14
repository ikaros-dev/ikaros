package run.ikaros.api.infra.exception.plugin;

import org.pf4j.PluginRuntimeException;

public class PluginInstallException extends PluginRuntimeException {
    public PluginInstallException() {
    }

    public PluginInstallException(String message) {
        super(message);
    }

    public PluginInstallException(Throwable cause) {
        super(cause);
    }

    public PluginInstallException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public PluginInstallException(String message, Object... args) {
        super(message, args);
    }
}
