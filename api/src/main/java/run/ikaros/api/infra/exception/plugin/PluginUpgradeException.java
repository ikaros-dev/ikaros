package run.ikaros.api.infra.exception.plugin;

import run.ikaros.api.infra.exception.IkarosPluginException;

public class PluginUpgradeException extends IkarosPluginException {
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
