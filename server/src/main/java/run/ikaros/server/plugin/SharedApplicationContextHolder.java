package run.ikaros.server.plugin;

import java.util.Map;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomSchemeManager;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.plugin.AllowPluginOperate;

/**
 * A holder for {@link SharedApplicationContext},
 * can register some beans to {@link SharedApplicationContext}
 * such as {@link ReactiveCustomClient}.
 *
 * @author li-guohao
 */
@Component
public class SharedApplicationContextHolder {


    private final ApplicationContext rootApplicationContext;
    private volatile SharedApplicationContext sharedApplicationContext;

    public SharedApplicationContextHolder(ApplicationContext rootApplicationContext) {
        this.rootApplicationContext = rootApplicationContext;
    }

    /**
     * Get shared applicationContext with single-instance-mode.
     *
     * @return a shared applicationContext
     */
    public SharedApplicationContext getInstance() {
        if (this.sharedApplicationContext == null) {
            synchronized (SharedApplicationContextHolder.class) {
                if (this.sharedApplicationContext == null) {
                    this.sharedApplicationContext = createSharedApplicationContext();
                }
            }
        }
        return this.sharedApplicationContext;
    }


    SharedApplicationContext createSharedApplicationContext() {
        // TODO optimize creation timing
        SharedApplicationContext sharedApplicationContext = new SharedApplicationContext();
        sharedApplicationContext.refresh();

        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) sharedApplicationContext.getBeanFactory();

        // Register shared object here
        ReactiveCustomClient reactiveCustomClient =
            rootApplicationContext.getBean(ReactiveCustomClient.class);
        beanFactory.registerSingleton("reactiveCustomClient", reactiveCustomClient);

        // Register custom scheme manager
        CustomSchemeManager customSchemeManager =
            rootApplicationContext.getBean(CustomSchemeManager.class);
        beanFactory.registerSingleton("schemeManager", customSchemeManager);

        // Register ikaros properties
        IkarosProperties ikarosProperties = rootApplicationContext.getBean(IkarosProperties.class);
        beanFactory.registerSingleton("ikarosProperties", ikarosProperties);

        // Register all allow plugin operate api.
        Map<String, AllowPluginOperate> allowPluginOperateMap =
            rootApplicationContext.getBeansOfType(AllowPluginOperate.class);
        for (String key : allowPluginOperateMap.keySet()) {
            beanFactory.registerSingleton(key, allowPluginOperateMap.get(key));
        }

        // Register web client
        WebClient webClient = rootApplicationContext.getBean(WebClient.class);
        beanFactory.registerSingleton("webClient", webClient);

        // TODO add more shared instance here

        return sharedApplicationContext;
    }
}
