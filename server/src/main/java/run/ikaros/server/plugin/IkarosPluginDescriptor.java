package run.ikaros.server.plugin;

import java.nio.file.Path;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.pf4j.DefaultPluginDescriptor;
import run.ikaros.api.plugin.custom.Plugin;

@Data
@EqualsAndHashCode(callSuper = true)
public class IkarosPluginDescriptor extends DefaultPluginDescriptor {
    private Plugin.Author author;
    private String logo;
    private String homepage;
    private String displayName;
    private Path loadLocation;

    public IkarosPluginDescriptor() {
    }

    public IkarosPluginDescriptor(String pluginId, String pluginDescription, String pluginClass,
                                  String version, String requires, String provider,
                                  String license) {
        super(pluginId, pluginDescription, pluginClass, version, requires, provider, license);
    }
}
