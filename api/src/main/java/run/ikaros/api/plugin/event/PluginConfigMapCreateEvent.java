package run.ikaros.api.plugin.event;

import lombok.Getter;
import run.ikaros.api.core.setting.ConfigMap;

@Getter
public class PluginConfigMapCreateEvent extends PluginConfigMapChangeEvent {

    public PluginConfigMapCreateEvent(Object source, String pluginId, ConfigMap configMap) {
        super(source, pluginId, configMap);
    }
}
