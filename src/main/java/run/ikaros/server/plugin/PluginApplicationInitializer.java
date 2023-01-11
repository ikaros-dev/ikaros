package run.ikaros.server.plugin;

import org.springframework.context.ApplicationContext;

public class PluginApplicationInitializer {
    private final IkarosPluginManager ikarosPluginManager;
    private final ApplicationContext rootApplicationContext;
    private final SharedApplicationContextHolder sharedApplicationContextHolder;

    /**
     * construct a plugin application initializer.
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
}
