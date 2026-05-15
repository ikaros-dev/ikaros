package run.ikaros.api.plugin.event;

import lombok.Getter;
import run.ikaros.api.core.setting.ConfigMap;

@Getter
public class PluginConfigMapChangeEvent extends PluginAwareEvent {
    private final ConfigMap configMap;

    public PluginConfigMapChangeEvent(Object source, String pluginId, ConfigMap configMap) {
        super(source, pluginId);
        this.configMap = configMap;
    }
}
