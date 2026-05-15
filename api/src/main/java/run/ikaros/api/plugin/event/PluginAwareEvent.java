package run.ikaros.api.plugin.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PluginAwareEvent extends ApplicationEvent {
    private String pluginId = "ALL";

    public PluginAwareEvent(Object source) {
        super(source);
    }

    public PluginAwareEvent(Object source, String pluginId) {
        super(source);
        this.pluginId = pluginId;
    }
}
