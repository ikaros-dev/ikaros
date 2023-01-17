package run.ikaros.server.plugin;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * <p>The default implementation for PluginFactory.</p>
 * <p>Get a {@link BasePlugin} instance from the {@link PluginApplicationContext}.</p>
 */
@Slf4j
public class BasePluginFactory implements PluginFactory {

    @Override
    public Plugin create(PluginWrapper pluginWrapper) {
        return getPluginContext(pluginWrapper)
            .map(context -> {
                try {
                    return context.getBean(BasePlugin.class);
                } catch (NoSuchBeanDefinitionException e) {
                    log.info(
                        "No bean named 'basePlugin' found in the context create default instance");
                    DefaultListableBeanFactory beanFactory =
                        context.getDefaultListableBeanFactory();
                    BasePlugin pluginInstance = new BasePlugin(pluginWrapper);
                    beanFactory.registerSingleton(Plugin.class.getName(), pluginInstance);
                    return pluginInstance;
                }
            })
            .orElse(null);
    }

    private Optional<PluginApplicationContext> getPluginContext(PluginWrapper pluginWrapper) {
        try {
            return Optional.of(PluginApplicationContextRegistry.getInstance())
                .map(registry -> registry.getByPluginId(pluginWrapper.getPluginId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
