package run.ikaros.server.plugin.event;

import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

public class IkarosPluginStoppedEvent extends ApplicationEvent {

    private final PluginWrapper plugin;

    public IkarosPluginStoppedEvent(Object source, PluginWrapper plugin) {
        super(source);
        this.plugin = plugin;
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }

    public PluginState getPluginState() {
        return plugin.getPluginState();
    }
}
