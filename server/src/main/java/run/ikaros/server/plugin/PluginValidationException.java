package run.ikaros.server.plugin;

import org.pf4j.PluginRuntimeException;

/**
 * 插件声明不符合服务端安全约束时抛出的校验异常.
 */
public class PluginValidationException extends PluginRuntimeException {
    public PluginValidationException(String message, Object... args) {
        super(message, args);
    }

    public PluginValidationException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }
}
