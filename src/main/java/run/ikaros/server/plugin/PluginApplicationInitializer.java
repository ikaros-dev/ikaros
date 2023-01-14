package run.ikaros.server.plugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
            new StopWatch(String.format("[%s]InitPluginAppContext", pluginId));

        stopWatch.start("CreatePluginAppContext");
        PluginApplicationContext pluginApplicationContext =
            createPluginApplicationContext(pluginId);
        stopWatch.stop();

        stopWatch.start("FindPluginCandidateComponents");
        Set<Class<?>> candidateComponents = findCandidateComponents(pluginId);
        stopWatch.stop();

        stopWatch.start("RegisterCandidateComponents");
        for (Class<?> component : candidateComponents) {
            log.debug("register a plugin component class [{}] to context for [{}]",
                component, pluginId);
            pluginApplicationContext.registerBean(component);
        }
        stopWatch.stop();

        stopWatch.start("RefreshPluginAppContext");
        pluginApplicationContext.refresh();
        stopWatch.stop();

        log.debug("[{}] initApplicationContext total millis: {} ms -> {}", pluginId,
            stopWatch.getTotalTimeMillis(), stopWatch.prettyPrint());

        contextRegistry.register(pluginId, pluginApplicationContext);
    }

    private PluginApplicationContext createPluginApplicationContext(String pluginId) {
        Assert.notNull(pluginId, "'pluginId' must not be null");
        PluginWrapper pluginWrapper = ikarosPluginManager.getPlugin(pluginId);
        ClassLoader pluginClassLoader = pluginWrapper.getPluginClassLoader();

        StopWatch stopWatch =
            new StopWatch("CreatePluginAppContext");

        stopWatch.start("CreatePluginAppContext");
        PluginApplicationContext pluginApplicationContext =
            new PluginApplicationContext(pluginId);
        pluginApplicationContext.setParent(sharedApplicationContextHolder.getInstance());
        pluginApplicationContext.setClassLoader(pluginClassLoader);
        pluginApplicationContext.getBeanFactory().setBeanClassLoader(pluginClassLoader);
        stopWatch.stop();

        stopWatch.start("CreatePluginDefaultResourceLoader");
        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader(pluginClassLoader);
        pluginApplicationContext.setResourceLoader(defaultResourceLoader);
        stopWatch.stop();



        stopWatch.start("RegisterPluginExtensionAnnotationPostProcessor");
        Class<?> cls;
        try {
            cls = pluginClassLoader.loadClass(pluginWrapper.getDescriptor().getPluginClass());
        } catch (ClassNotFoundException e) {
            throw new PluginException("load plugin class fail", e);
        }
        PluginExtensionAnnotationPostProcessor pluginExtensionAnnotationPostProcessor
            = new PluginExtensionAnnotationPostProcessor(cls.getPackageName());
        pluginExtensionAnnotationPostProcessor.postProcessBeanDefinitionRegistry(
            pluginApplicationContext);
        stopWatch.stop();

        stopWatch.start("RegisterAnnotationConfigProcessors");
        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) pluginApplicationContext.getBeanFactory();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
        stopWatch.stop();

        beanFactory.registerSingleton("pluginWrapper", pluginWrapper);

        // createBasePluginBeanIfNotExists(pluginApplicationContext, pluginWrapper);

        log.debug("[{}] Total millis: {} ms -> {}", pluginId, stopWatch.getTotalTimeMillis(),
            stopWatch.prettyPrint());

        return pluginApplicationContext;
    }

    @SuppressWarnings("unchecked")
    private void createBasePluginBeanIfNotExists(PluginApplicationContext pluginApplicationContext,
                                                 PluginWrapper pluginWrapper) {
        ConfigurableListableBeanFactory beanFactory = pluginApplicationContext.getBeanFactory();
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        String pluginClass = pluginDescriptor.getPluginClass();
        ClassLoader pluginClassLoader = pluginWrapper.getPluginClassLoader();
        Class<BasePlugin> cls = null;
        try {

            cls = (Class<BasePlugin>) pluginClassLoader.loadClass(pluginClass);
            beanFactory.getBean(cls);
        } catch (NoSuchBeanDefinitionException noSuchBeanDefinitionException) {
            try {
                BasePlugin plugin =
                    Objects.requireNonNull(cls).getDeclaredConstructor(PluginWrapper.class)
                        .newInstance(pluginWrapper);
                beanFactory.registerSingleton(pluginDescriptor.getPluginId(), plugin);
            } catch (Exception e) {
                throw new PluginException("create BasePlugin Bean fail", e);
            }
        } catch (ClassNotFoundException e) {
            throw new PluginException("class not found for class: " + pluginClass, e);
        }
    }

    private Set<Class<?>> findCandidateComponents(String pluginId) {
        Assert.notNull(pluginId, "'pluginId' must not be null");
        StopWatch stopWatch =
            new StopWatch("FindPluginCandidateComponents");

        stopWatch.start("GetExtensionClassNames");
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

        log.debug("[{}] total millis: {}ms -> {}", pluginId,
            stopWatch.getTotalTimeMillis(), stopWatch.prettyPrint());
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
