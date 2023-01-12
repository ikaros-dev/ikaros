package run.ikaros.server.plugin;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * Plugin application initializer will
 * <br/> register beans that can be accessed by plugin in {@link SharedApplicationContextHolder}
 * <br/> and
 * <br/> create plugin application context by plugin id.
 *
 * @author li-guohao
 * @see SharedApplicationContextHolder
 * @see IkarosPluginManager
 */
@Slf4j
public class PluginApplicationInitializer {
    private final PluginApplicationContextRegistry contextRegistry =
        PluginApplicationContextRegistry.getInstance();
    private final IkarosPluginManager ikarosPluginManager;
    private final ApplicationContext rootApplicationContext;
    private final SharedApplicationContextHolder sharedApplicationContextHolder;

    /**
     * Construct a plugin application initializer.
     *
     * @param ikarosPluginManager    ikaros plugin manager
     * @param rootApplicationContext ikaros root application context
     */
    public PluginApplicationInitializer(IkarosPluginManager ikarosPluginManager,
                                        ApplicationContext rootApplicationContext) {
        this.ikarosPluginManager = ikarosPluginManager;
        this.rootApplicationContext = rootApplicationContext;
        this.sharedApplicationContextHolder =
            rootApplicationContext.getBean(SharedApplicationContextHolder.class);
    }

    public PluginApplicationContext getPluginApplicationContext(String pluginId) {
        return contextRegistry.getByPluginId(pluginId);
    }


    public void onStartUp(String pluginId) {
        initPluginApplicationContext(pluginId);
    }

    /**
     * Create plugin application when plugin load.
     *
     * @param pluginId plugin id
     */
    public void initPluginApplicationContext(String pluginId) {
        if (contextRegistry.containsContext(pluginId)) {
            log.debug("plugin application context for [{}] has bean initialized.", pluginId);
            return;
        }

        StopWatch stopWatch =
            new StopWatch(String.format("init-plugin-application-context-%s", pluginId));

        stopWatch.start(String.format("create plugin application context for [%s] ", pluginId));
        PluginApplicationContext pluginApplicationContext =
            createPluginApplicationContext(pluginId);
        stopWatch.stop();

        stopWatch.start(String.format("find plugin candidate components for [%s] ", pluginId));
        Set<Class<?>> candidateComponents = findCandidateComponents(pluginId);
        stopWatch.stop();

        stopWatch.start(
            String.format("register all plugin candidate component bean for [%s] ", pluginId));
        for (Class<?> component : candidateComponents) {
            log.debug("register a plugin component class [{}] to context for [{}]",
                component, pluginId);
            pluginApplicationContext.registerBean(component);
        }
        stopWatch.stop();

        stopWatch.start(String.format("refresh plugin application context for [%s] ", pluginId));
        pluginApplicationContext.refresh();
        stopWatch.stop();

        log.debug("initApplicationContext total millis: {} ms -> {} for [{}]",
            stopWatch.getTotalTimeMillis(), stopWatch.prettyPrint(), pluginId);
    }

    private PluginApplicationContext createPluginApplicationContext(String pluginId) {
        Assert.notNull(pluginId, "'pluginId' must not be null");
        PluginWrapper plugin = ikarosPluginManager.getPlugin(pluginId);
        ClassLoader pluginClassLoader = plugin.getPluginClassLoader();


        StopWatch stopWatch =
            new StopWatch(String.format("create-plugin-application-context-%s", pluginId));

        stopWatch.start(String.format("create plugin application context for [%s]", pluginId));
        PluginApplicationContext pluginApplicationContext =
            new PluginApplicationContext(pluginId, sharedApplicationContextHolder.getInstance());
        pluginApplicationContext.setClassLoader(pluginClassLoader);
        stopWatch.stop();

        stopWatch.start(String.format("create plugin default resource loader for [%s]", pluginId));
        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader(pluginClassLoader);
        pluginApplicationContext.setResourceLoader(defaultResourceLoader);
        stopWatch.stop();

        stopWatch.start(String.format("register annotation config processors for [%s]", pluginId));
        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) pluginApplicationContext.getBeanFactory();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
        stopWatch.stop();

        beanFactory.registerSingleton("pluginWrapper", ikarosPluginManager.getPlugin(pluginId));

        log.debug("Total millis: {} ms -> {} for [{}]", stopWatch.getTotalTimeMillis(),
            stopWatch.prettyPrint(), pluginId);

        contextRegistry.register(pluginId, pluginApplicationContext);
        return pluginApplicationContext;
    }

    private Set<Class<?>> findCandidateComponents(String pluginId) {
        Assert.notNull(pluginId, "'pluginId' must not be null");
        StopWatch stopWatch =
            new StopWatch(String.format("find-plugin-candidate-components-%s", pluginId));

        stopWatch.start(String.format("get extension class names for [%s]", pluginId));
        Set<String> extensionClassNames = ikarosPluginManager.getExtensionClassNames(pluginId);
        if (extensionClassNames == null) {
            log.debug("No components class names found for plugin [{}]", pluginId);
            extensionClassNames = Set.of();
        }
        stopWatch.stop();

        log.debug("Add all extensions as beans for [{}]", pluginId);
        PluginWrapper plugin = ikarosPluginManager.getPlugin(pluginId);
        Set<Class<?>> candidateComponents = new HashSet<>();
        for (String extensionClassName : extensionClassNames) {
            log.debug("Load extension class '{}'", extensionClassName);
            try {
                stopWatch.start("loadClass");
                Class<?> extensionClass =
                    plugin.getPluginClassLoader().loadClass(extensionClassName);
                stopWatch.stop();

                candidateComponents.add(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        log.debug("total millis: {}ms -> {} for [{}]",
            stopWatch.getTotalTimeMillis(), stopWatch.prettyPrint(), pluginId);
        return candidateComponents;
    }

    /**
     * plugin application context destroyed.
     *
     * @param pluginId plugin id
     */
    public void contextDestroyed(String pluginId) {
        Assert.notNull(pluginId, "'pluginId' must not be null");
        PluginApplicationContext removed = contextRegistry.remove(pluginId);
        if (removed != null) {
            removed.close();
        }
    }
}
