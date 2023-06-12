package run.ikaros.server.core.plugin;

public enum PluginStateOperate {
    START,
    STOP,
    ENABLE,
    DISABLE,
    LOAD,
    LOAD_ALL(),
    RELOAD,
    RELOAD_ALL,
    RELOAD_ALL_STARTED,
    DELETE,
    UNLOAD
}
