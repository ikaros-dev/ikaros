package run.ikaros.server.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
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

    private IkarosPluginDescriptor convert(Plugin plugin) {
        IkarosPluginDescriptor pluginDescriptor =
            new IkarosPluginDescriptor(plugin.getName(),
                plugin.getDescription(),
                plugin.getClazz(),
                plugin.getVersion(),
                plugin.getRequires(),
                plugin.getAuthor().getName(),
                plugin.getLicense());
        pluginDescriptor.setAuthor(plugin.getAuthor());
        pluginDescriptor.setLogo(plugin.getLogo());
        pluginDescriptor.setHomepage(plugin.getHomepage());
        pluginDescriptor.setDisplayName(plugin.getDisplayName());
        pluginDescriptor.setLoadLocation(plugin.getLoadLocation());
        pluginDescriptor.setConfigMapSchemas(plugin.getConfigMapSchemas());

        // add dependencies
        plugin.getDependencies().forEach((pluginDepName, versionRequire) -> {
            PluginDependency dependency =
                new PluginDependency(String.format("%s@%s", pluginDepName, versionRequire));
            pluginDescriptor.addDependency(dependency);
        });
        return pluginDescriptor;
    }


}
