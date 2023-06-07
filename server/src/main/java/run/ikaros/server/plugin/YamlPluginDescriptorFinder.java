package run.ikaros.server.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.util.FileUtils;
import run.ikaros.api.plugin.custom.Plugin;

@Slf4j
public class YamlPluginDescriptorFinder implements PluginDescriptorFinder {

    private final YamlPluginFinder yamlPluginFinder;
    private final IkarosPluginManager pluginManager;

    public YamlPluginDescriptorFinder(IkarosPluginManager pluginManager) {
        this.yamlPluginFinder = new YamlPluginFinder(pluginManager);
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath)
            && (Files.isDirectory(pluginPath)
            || FileUtils.isJarFile(pluginPath));
    }

    @Override
    public PluginDescriptor find(Path pluginPath) {
        Plugin plugin = yamlPluginFinder.find(pluginPath);
        return convert(plugin);
    }

    private DefaultPluginDescriptor convert(Plugin plugin) {
        DefaultPluginDescriptor defaultPluginDescriptor =
            new DefaultPluginDescriptor(plugin.getName(),
                plugin.getDescription(),
                plugin.getClazz(),
                plugin.getVersion(),
                plugin.getRequires(),
                plugin.getAuthor().getName(),
                plugin.getLicense());

        // add dependencies
        plugin.getDependencies().forEach((pluginDepName, versionRequire) -> {
            PluginDependency dependency =
                new PluginDependency(String.format("%s@%s", pluginDepName, versionRequire));
            defaultPluginDescriptor.addDependency(dependency);
        });
        return defaultPluginDescriptor;
    }


}
