package run.ikaros.server.plugin.event;

import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

public class IkarosPluginStartedEvent extends ApplicationEvent {

    private final PluginWrapper plugin;


    public IkarosPluginStartedEvent(Object source, PluginWrapper plugin) {
        super(source);
        this.plugin = plugin;
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }
}
