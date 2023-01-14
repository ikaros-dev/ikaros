package run.ikaros.server.plugin;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class PluginPropertiesEnablesInit implements InitializingBean {

    private final IkarosPluginManager ikarosPluginManager;
    private final PluginProperties pluginProperties;

    public PluginPropertiesEnablesInit(IkarosPluginManager ikarosPluginManager,
                                       PluginProperties pluginProperties) {
        this.ikarosPluginManager = ikarosPluginManager;
        this.pluginProperties = pluginProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ikarosPluginManager.loadPlugins();
        for (String pluginId : pluginProperties.getEnabledPlugins()) {
            ikarosPluginManager.startPlugin(pluginId);
        }
    }

}
