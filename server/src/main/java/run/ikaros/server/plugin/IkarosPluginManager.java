package run.ikaros.server.plugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.ExtensionFinder;
import org.pf4j.Plugin;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginRepository;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import run.ikaros.server.plugin.event.IkarosPluginDeleteEvent;

/**
 * Ikaros plugin manager.
 *
 * @author: li-guohao
 */
@Slf4j
public class IkarosPluginManager extends DefaultPluginManager
    implements ApplicationContextAware, InitializingBean, DisposableBean {
    private final Map<String, PluginStartingError> startingErrors = new HashMap<>();

    private ApplicationContext rootApplicationContext;
    private PluginApplicationInitializer pluginApplicationInitializer;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.rootApplicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.pluginApplicationInitializer
            = new PluginApplicationInitializer(this,
            rootApplicationContext.getBean(SharedApplicationContextHolder.class));
    }

    @Override
    public void destroy() {
        stopPlugins();
    }

    final PluginApplicationContext getPluginApplicationContext(String pluginId) {
        return pluginApplicationInitializer.getPluginApplicationContext(pluginId);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new IkarosExtensionFactory(this);
    }

    @Override
    protected ExtensionFinder createExtensionFinder() {
        return new IkarosExtensionFinder(this);
    }

    public PluginStartingError getPluginStartingError(String pluginId) {
        return startingErrors.get(pluginId);
    }

    public void clearPluginStaringError() {
        startingErrors.clear();
    }

    @Override
    protected PluginFactory createPluginFactory() {
        return new BasePluginFactory();
    }

    public PluginRepository getPluginRepository() {
        return this.pluginRepository;
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new YamlPluginDescriptorFinder(this);
    }

    @Override
    protected PluginState stopPlugin(String pluginId, boolean stopDependents) {
        checkPluginId(pluginId);
        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STOPPED == pluginState) {
            log.debug("Already stopped plugin '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STOPPED;
        }

        // test for disabled plugin
        if (PluginState.DISABLED == pluginState) {
            // do nothing
            return pluginState;
        }

        if (stopDependents) {
            List<String> dependents = dependencyResolver.getDependents(pluginId);
            while (!dependents.isEmpty()) {
                String dependent = dependents.remove(0);
                stopPlugin(dependent, false);
                dependents.addAll(0, dependencyResolver.getDependents(dependent));
            }
        }
        try {
            log.info("Stop plugin '{}'", getPluginLabel(pluginDescriptor));
            pluginWrapper.getPlugin().stop();
            pluginWrapper.setPluginState(PluginState.STOPPED);

            // get an instance of plugin before the plugin is unloaded
            // for reason see https://github.com/pf4j/pf4j/issues/309
            Plugin plugin = pluginWrapper.getPlugin();

            // notify the plugin as it's deleted
            if (Objects.nonNull(plugin)) {
                plugin.delete();
            }

            // release plugin resources
            releaseAdditionalResources(pluginId);

            startedPlugins.remove(pluginWrapper);

            firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
        }
        return pluginWrapper.getPluginState();
    }

    @Override
    public PluginState stopPlugin(String pluginId) {
        return this.stopPlugin(pluginId, true);
    }

    @Override
    public void startPlugins() {
        startingErrors.clear();
        long ts = System.currentTimeMillis();

        for (PluginWrapper pluginWrapper : resolvedPlugins) {
            // checkExtensionFinderReady(pluginWrapper);
            PluginState pluginState = pluginWrapper.getPluginState();
            if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
                try {
                    log.info("Start plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    // inject bean
                    pluginApplicationInitializer.onStartUp(pluginWrapper.getPluginId());

                    pluginWrapper.getPlugin().start();

                    pluginWrapper.setPluginState(PluginState.STARTED);
                    pluginWrapper.setFailedException(null);
                    startedPlugins.add(pluginWrapper);

                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                } catch (Exception | LinkageError e) {
                    pluginWrapper.setPluginState(PluginState.FAILED);
                    pluginWrapper.setFailedException(e);
                    startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                        pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
                    releaseAdditionalResources(pluginWrapper.getPluginId());
                    log.error("Unable to start plugin '{}'",
                        getPluginLabel(pluginWrapper.getDescriptor()), e);
                }
            }
        }

        log.info("[Ikaros] {} plugins are started in {}ms. {} failed",
            getPlugins(PluginState.STARTED).size(),
            System.currentTimeMillis() - ts, startingErrors.size());
    }

    @Override
    public PluginState startPlugin(String pluginId) {
        return doStartPlugin(pluginId);
    }

    @Override
    public void stopPlugins() {
        doStopPlugins();
    }

    private PluginState doStartPlugin(String pluginId) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STARTED == pluginState) {
            log.debug("Already started plugin '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STARTED;
        }

        if (!resolvedPlugins.contains(pluginWrapper)) {
            log.warn("Cannot start an unresolved plugin '{}'", getPluginLabel(pluginDescriptor));
            return pluginState;
        }

        if (PluginState.DISABLED == pluginState) {
            // automatically enable plugin on manual plugin start
            if (!enablePlugin(pluginId)) {
                return pluginState;
            }
        }

        for (PluginDependency dependency : pluginDescriptor.getDependencies()) {
            // start dependency only if it marked as required (non-optional) or if it's optional
            // and loaded
            if (!dependency.isOptional() || plugins.containsKey(dependency.getPluginId())) {
                startPlugin(dependency.getPluginId());
            }
        }
        log.info("Start plugin '{}'", getPluginLabel(pluginDescriptor));

        try {
            // load and inject bean
            pluginApplicationInitializer.onStartUp(pluginId);

            // create plugin instance and start it
            pluginWrapper.getPlugin().start();

            // requestMappingManager.registerHandlerMappings(pluginWrapper);

            pluginWrapper.setPluginState(PluginState.STARTED);
            startedPlugins.add(pluginWrapper);

            firePluginStateEvent(
                new PluginStateEvent(this, pluginWrapper, pluginWrapper.getPluginState()));
        } catch (Exception e) {
            log.error("Unable to start plugin '{}'",
                getPluginLabel(pluginWrapper.getDescriptor()), e);
            pluginWrapper.setPluginState(PluginState.FAILED);
            startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
            releaseAdditionalResources(pluginId);
        }
        return pluginWrapper.getPluginState();
    }


    private void doStopPlugins() {
        startingErrors.clear();
        // stop started plugins in reverse order
        Collections.reverse(startedPlugins);
        Iterator<PluginWrapper> itr = startedPlugins.iterator();
        while (itr.hasNext()) {
            PluginWrapper pluginWrapper = itr.next();
            PluginState pluginState = pluginWrapper.getPluginState();
            if (PluginState.STARTED == pluginState) {
                try {
                    log.info("Stop plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    pluginWrapper.getPlugin().stop();
                    pluginWrapper.setPluginState(PluginState.STOPPED);
                    itr.remove();
                    releaseAdditionalResources(pluginWrapper.getPluginId());

                } catch (PluginRuntimeException e) {
                    log.error(e.getMessage(), e);
                    startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                        pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
                } finally {
                    firePluginStateEvent(
                        new PluginStateEvent(this, pluginWrapper, pluginWrapper.getPluginState()));
                }
            }
        }
    }

    /**
     * Unload all plugin and restart.
     */
    public void reloadPlugins() {
        getPlugins().forEach(pluginWrapper -> reloadPlugin(pluginWrapper.getPluginId()));
    }

    /**
     * Unload all has started plugin and restart.
     */
    public void reloadStartedPlugins() {
        getPlugins(PluginState.STARTED)
            .forEach(pluginWrapper -> reloadPlugin(pluginWrapper.getPluginId()));
    }

    /**
     * Reload plugin by id,it will be clean up memory resources of plugin and reload plugin from
     * disk.
     *
     * @param pluginId plugin id
     * @return plugin startup status
     */
    public PluginState reloadPlugin(String pluginId) {
        PluginWrapper plugin = getPlugin(pluginId);
        stopPlugin(pluginId, false);
        unloadPlugin(pluginId, false);
        try {
            loadPlugin(plugin.getPluginPath());
        } catch (Exception ex) {
            return null;
        }

        return doStartPlugin(pluginId);
    }

    /**
     * Release plugin holding release on stop.
     */
    public void releaseAdditionalResources(String pluginId) {
        try {
            pluginApplicationInitializer.contextDestroyed(pluginId);
        } catch (Exception e) {
            log.debug("Plugin application context close failed. ", e);
        }
    }

    /**
     * Load plugin by id.
     *
     * @param pluginId plugin id.
     * @return {@link PluginState}
     */
    public PluginState loadPlugin(String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text.");
        Path pluginPath = getPlugin(pluginId).getPluginPath();
        loadPlugin(pluginPath);
        return getPlugin(pluginId).getPluginState();
    }

    @Override
    protected PluginWrapper loadPluginFromPath(Path pluginPath) {
        return super.loadPluginFromPath(pluginPath);
    }

    @Override
    public boolean deletePlugin(String pluginId) {
        Assert.hasText(pluginId, "'pluginId' must has text.");
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        // stop the plugin if it's started
        PluginState pluginState = stopPlugin(pluginId);
        if (PluginState.STARTED == pluginState) {
            log.error("Failed to stop plugin '{}' on delete", pluginId);
            return false;
        }

        if (!unloadPlugin(pluginId)) {
            log.error("Failed to unload plugin '{}' on delete", pluginId);
            return false;
        }

        // delete plugin path.
        Path pluginPath = pluginWrapper.getPluginPath();
        boolean result = pluginRepository.deletePluginPath(pluginPath);

        firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, null));
        // publish plugin deleted event.
        rootApplicationContext.publishEvent(new IkarosPluginDeleteEvent(this, pluginId));

        return result;
    }

    @Override
    protected void resolvePlugins() {
        super.resolvePlugins();
    }
}
