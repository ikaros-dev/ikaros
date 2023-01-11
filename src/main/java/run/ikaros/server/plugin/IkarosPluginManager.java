package run.ikaros.server.plugin;

import java.nio.file.Path;
import java.util.List;
import org.pf4j.DefaultPluginManager;
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

    public IkarosPluginManager() {
    }

    public IkarosPluginManager(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    public IkarosPluginManager(List<Path> pluginsRoots) {
        super(pluginsRoots);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.rootApplicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.pluginApplicationInitializer
            = new PluginApplicationInitializer(this, rootApplicationContext);
    }


}
