package run.ikaros.api.plugin.event;

import lombok.Getter;
import run.ikaros.api.core.setting.ConfigMap;

@Getter
public class PluginConfigMapUpdateEvent extends PluginConfigMapChangeEvent {
    public PluginConfigMapUpdateEvent(Object source, String pluginId, ConfigMap configMap) {
        super(source, pluginId, configMap);
    }
}
