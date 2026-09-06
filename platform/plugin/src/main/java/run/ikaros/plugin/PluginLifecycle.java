package run.ikaros.plugin;

/** 插件运行时生命周期。 */
public enum PluginLifecycle {
    DISCOVERED, INSTALLED, ENABLED, DISABLED, FAILED, INCOMPATIBLE, UNINSTALLED
}
