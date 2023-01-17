package run.ikaros.server.plugin.event;

import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

public class IkarosPluginLoadedEvent extends ApplicationEvent {
    private final PluginWrapper pluginWrapper;


    public IkarosPluginLoadedEvent(Object source, PluginWrapper pluginWrapper) {
        super(source);
        this.pluginWrapper = pluginWrapper;
    }

    public PluginWrapper getPluginWrapper() {
        return pluginWrapper;
    }
}
