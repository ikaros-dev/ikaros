package run.ikaros.server.plugin.event;

import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

/**
 * Plugin state changed event.
 */
public class IkarosPluginStateChangedEvent extends ApplicationEvent {

    private final PluginWrapper plugin;

    private final PluginState oldState;

    /**
     * Construct instance.
     *
     * @param source   publisher
     * @param wrapper  plugin wrapper
     * @param oldState old plugin state
     */
    public IkarosPluginStateChangedEvent(Object source, PluginWrapper wrapper,
                                         PluginState oldState) {
        super(source);
        this.plugin = wrapper;
        this.oldState = oldState;
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }

    public PluginState getOldState() {
        return oldState;
    }

    public PluginState getState() {
        return this.plugin.getPluginState();
    }
}
