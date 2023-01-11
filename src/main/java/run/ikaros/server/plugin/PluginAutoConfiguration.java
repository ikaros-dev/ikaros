package run.ikaros.server.plugin;

import java.io.File;
import org.pf4j.RuntimeMode;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(PluginProperties.class)
public class PluginAutoConfiguration {
    private final PluginProperties pluginProperties;

    public PluginAutoConfiguration(PluginProperties pluginProperties) {
        this.pluginProperties = pluginProperties;
    }

    /**
     * New a {@link IkarosPluginManager} instance to manager plugin.
     *
     * @return a {@link IkarosPluginManager} instance
     */
    @Bean
    public IkarosPluginManager ikarosPluginManager() {
        // Setup RuntimeMode
        System.setProperty("pf4j.mode", pluginProperties.getRuntimeMode().toString());
        // Setup Plugin folder
        String pluginsRoot =
            StringUtils.hasText(pluginProperties.getPluginsRoot())
                ? pluginProperties.getPluginsRoot()
                : "plugins";
        System.setProperty("pf4j.pluginsDir", pluginsRoot);
        String appHome = System.getProperty("app.home");
        if (RuntimeMode.DEPLOYMENT == pluginProperties.getRuntimeMode()
            && StringUtils.hasText(appHome)) {
            System.setProperty("pf4j.pluginsDir", appHome + File.separator + pluginsRoot);
        }
        // New instance
        return new IkarosPluginManager(pluginProperties);
    }
}
