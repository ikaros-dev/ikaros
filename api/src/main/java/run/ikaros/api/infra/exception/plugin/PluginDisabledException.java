package run.ikaros.api.infra.exception.plugin;

import run.ikaros.api.infra.exception.IkarosPluginException;

public class PluginDisabledException extends IkarosPluginException {
    public PluginDisabledException() {
    }

    public PluginDisabledException(String message) {
        super(message);
    }

    public PluginDisabledException(Throwable cause) {
        super(cause);
    }

    public PluginDisabledException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public PluginDisabledException(String message, Object... args) {
        super(message, args);
    }
}
