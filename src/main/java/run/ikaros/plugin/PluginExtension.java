package run.ikaros.plugin;

/** 已启用插件向平台注册的扩展点绑定。 */
public record PluginExtension(String pluginId, String extensionPoint, String entrypoint) { }
