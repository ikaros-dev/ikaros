package run.ikaros.api.infra.exception.plugin;

import org.pf4j.PluginRuntimeException;

public class PluginUpgradeException extends PluginRuntimeException {
    public PluginUpgradeException() {
    }

    public PluginUpgradeException(String message) {
        super(message);
    }


    public PluginUpgradeException(Throwable cause) {
        super(cause);
    }

    public PluginUpgradeException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public PluginUpgradeException(String message, Object... args) {
        super(message, args);
    }
}
