package run.ikaros.server.plugin;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.CompoundPluginRepository;
import org.pf4j.DefaultPluginManager;
import org.pf4j.DefaultPluginRepository;
import org.pf4j.JarPluginRepository;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.PluginRepository;
import org.pf4j.PluginStatusProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

/**
 * Ikaros plugin manager.
 *
 * @author: li-guohao
 */
public class IkarosPluginManager extends DefaultPluginManager
    implements ApplicationContextAware, InitializingBean {

    private ApplicationContext rootApplicationContext;
    private PluginApplicationInitializer pluginApplicationInitializer;

    private final PluginProperties pluginProperties;

    public IkarosPluginManager(PluginProperties pluginProperties) {
        super();
        this.pluginProperties = pluginProperties;
    }

    public IkarosPluginManager(PluginProperties pluginProperties, Path... pluginsRoots) {
        super(pluginsRoots);
        this.pluginProperties = pluginProperties;
    }

    public IkarosPluginManager(PluginProperties pluginProperties, List<Path> pluginsRoots) {
        super(pluginsRoots);
        this.pluginProperties = pluginProperties;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.rootApplicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        initAttrByPluginProperties();
        this.pluginApplicationInitializer
            = new PluginApplicationInitializer(this, rootApplicationContext);
    }

    private void initAttrByPluginProperties() {
        setExactVersionAllowed(pluginProperties.isExactVersionAllowed());
        setSystemVersion(pluginProperties.getSystemVersion());
    }

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
        if (PropertyPluginStatusProvider.isPropertySet(pluginProperties)) {
            return new PropertyPluginStatusProvider(pluginProperties);
        }
        return super.createPluginStatusProvider();
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



}
