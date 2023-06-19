package run.ikaros.server.plugin;

import java.lang.reflect.Constructor;
import java.util.Objects;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.CompoundPluginRepository;
import org.pf4j.DefaultPluginRepository;
import org.pf4j.JarPluginRepository;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.PluginRepository;
import org.pf4j.PluginStatusProvider;
import org.pf4j.RuntimeMode;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.plugin.listener.PluginStateChangedListener;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PluginProperties.class)
public class PluginConfiguration {

    @Bean
    public PluginStateChangedListener pluginStateChangedListener(
        ReactiveCustomClient reactiveCustomClient) {
        return new PluginStateChangedListener(reactiveCustomClient);
    }

    /**
     * New a {@link IkarosPluginManager} instance to manager plugin.
     *
     * @return a {@link IkarosPluginManager} instance
     */
    @Bean
    public IkarosPluginManager ikarosPluginManager(PluginProperties pluginProperties,
                                                   IkarosProperties ikarosProperties,
                                                   PluginStateChangedListener
                                                       pluginStateChangedListener) {
        // Setup RuntimeMode
        RuntimeMode runtimeMode = RuntimeMode.DEPLOYMENT;
        if (Objects.nonNull(pluginProperties.getRuntimeMode())) {
            runtimeMode = pluginProperties.getRuntimeMode();
        }
        System.setProperty("pf4j.mode", runtimeMode.toString());

        // Setup Plugin folder
        String pluginsRoot = pluginProperties.getPluginsRoot();
        if (!StringUtils.hasText(pluginsRoot)) {
            pluginsRoot = ikarosProperties.getWorkDir().resolve("plugins").toString();
        }
        System.setProperty("pf4j.pluginsDir", pluginsRoot);

        // New instance
        IkarosPluginManager ikarosPluginManager = new IkarosPluginManager() {
            @Override
            protected PluginLoader createPluginLoader() {
                if (pluginProperties.getCustomPluginLoader() != null) {
                    Class<PluginLoader> clazz = pluginProperties.getCustomPluginLoader();
                    try {
                        Constructor<?> constructor = clazz.getConstructor(PluginManager.class);
                        return (PluginLoader) constructor.newInstance(this);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException(
                            String.format("Create custom PluginLoader %s failed. Make sure"
                                    + "there is a constructor with one argument that accepts "
                                    + "PluginLoader",
                                clazz.getName()));
                    }
                } else {
                    return new CompoundPluginLoader()
                        .add(new IkarosDevelopmentPluginLoader(this), this::isDevelopment)
                        .add(new IkarosJarPluginLoader(this), this::isNotDevelopment);
                }
            }

            @Override
            protected PluginStatusProvider createPluginStatusProvider() {
                return new PropertyPluginStatusProvider(pluginProperties);
            }

            @Override
            protected PluginRepository createPluginRepository() {
                var fixedPathDevelopmentPluginRepository =
                    new FixedPathDevelopmentPluginRepository(getPluginsRoots());
                fixedPathDevelopmentPluginRepository
                    .setFixedPaths(pluginProperties.getFixedPluginPath());
                return new CompoundPluginRepository()
                    .add(fixedPathDevelopmentPluginRepository, this::isDevelopment)
                    .add(new JarPluginRepository(getPluginsRoots()), this::isNotDevelopment)
                    .add(new DefaultPluginRepository(getPluginsRoots()),
                        this::isNotDevelopment);
            }
        };

        // Setup others properties
        ikarosPluginManager.setExactVersionAllowed(pluginProperties.isExactVersionAllowed());
        ikarosPluginManager.setSystemVersion(pluginProperties.getSystemVersion());

        // Add listener
        ikarosPluginManager.addPluginStateListener(pluginStateChangedListener);
        // Config plugin state change listener manager ref.
        pluginStateChangedListener.setIkarosPluginManager(ikarosPluginManager);
        return ikarosPluginManager;
    }
}
