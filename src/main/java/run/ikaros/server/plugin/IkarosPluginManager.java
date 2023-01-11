package run.ikaros.server.plugin;

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
