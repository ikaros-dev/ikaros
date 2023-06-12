package run.ikaros.server.plugin.event;

import org.springframework.context.ApplicationEvent;

public class IkarosPluginDeleteEvent extends ApplicationEvent {
    private final String pluginId;

    public IkarosPluginDeleteEvent(Object source, String pluginId) {
        super(source);
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }
}
