package run.ikaros.server.plugin;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;

@Slf4j
public class YamlPluginDescriptorFinder implements PluginDescriptorFinder {
    @Override
    public boolean isApplicable(Path pluginPath) {
        // todo impl
        return false;
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        // todo impl
        return null;
    }
}
