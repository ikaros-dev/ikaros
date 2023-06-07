package run.ikaros.api.plugin;

public interface PluginConst {

    String PLUGIN_NAME_LABEL_NAME = "plugin.ikaros.run/plugin-name";

    String SYSTEM_PLUGIN_NAME = "system";

    String RELOAD_ANNO = "plugin.ikaros.run/reload";

    String PLUGIN_PATH = "plugin.ikaros.run/plugin-path";

    static String assertsRoutePrefix(String pluginName) {
        return "/plugins/" + pluginName + "/assets/";
    }

    String STATIC_RESOURCE_DIR_CONSOLE = "console";
    String STATIC_RESOURCE_DIR_STATIC = "static";

}
