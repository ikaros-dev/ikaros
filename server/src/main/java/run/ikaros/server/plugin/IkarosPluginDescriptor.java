package run.ikaros.server.plugin;

import java.nio.file.Path;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.pf4j.DefaultPluginDescriptor;
import run.ikaros.api.plugin.custom.Plugin;

@Data
@EqualsAndHashCode(callSuper = true)
public class IkarosPluginDescriptor extends DefaultPluginDescriptor {
    private Plugin.@Nullable Author author;
    private @Nullable String logo;
    private @Nullable String homepage;
    private @Nullable String displayName;
    private @Nullable Path loadLocation;
    private @Nullable String configMapSchemas;

    public IkarosPluginDescriptor(String pluginId, String pluginDescription, String pluginClass,
                                  String version, String requires, String provider,
                                  String license) {
        super(pluginId, pluginDescription, pluginClass, version, requires, provider, license);
    }
}
