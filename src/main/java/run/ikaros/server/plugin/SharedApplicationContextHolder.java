package run.ikaros.server.plugin;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import run.ikaros.server.custom.ReactiveCustomClient;

/**
 * a holder for {@link SharedApplicationContext},
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
     * get shared applicationContext with single-instance-mode.
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

        // register shared object here;
        ReactiveCustomClient reactiveCustomClient =
            rootApplicationContext.getBean(ReactiveCustomClient.class);
        beanFactory.registerSingleton("reactiveCustomClient", reactiveCustomClient);

        // TODO add more shared instance here

        return sharedApplicationContext;
    }
}
