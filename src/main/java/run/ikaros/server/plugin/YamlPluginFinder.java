package run.ikaros.server.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DevelopmentPluginClasspath;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.util.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import run.ikaros.server.core.custom.Plugin;

/**
 * Reading plugin descriptor data from plugin.yaml.
 *
 * @see Plugin
 */
@Slf4j
public class YamlPluginFinder {
    static final DevelopmentPluginClasspath PLUGIN_CLASSPATH = new DevelopmentPluginClasspath();
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "plugin.yaml";
    private final String propertiesFileName;

    public YamlPluginFinder() {
        this(DEFAULT_PROPERTIES_FILE_NAME);
    }

    public YamlPluginFinder(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    /**
     * find plugin.
     *
     * @param pluginPath path
     * @return plugin
     */
    public Plugin find(Path pluginPath) {
        Plugin plugin = readPluginDescriptor(pluginPath);
        if (plugin.getState() == null) {
            plugin.setState(PluginState.RESOLVED);
        }
        return plugin;
    }

    private Plugin readPluginDescriptor(Path pluginPath) {
        Path propertiesPath = getManifestPath(pluginPath, propertiesFileName);
        if (propertiesPath == null) {
            throw new PluginRuntimeException("Cannot find the plugin manifest path");
        }

        log.debug("Lookup plugin descriptor in '{}'", propertiesPath);
        if (Files.notExists(propertiesPath)) {
            throw new PluginRuntimeException("Cannot find '{}' in resource path for plugin: '{}'",
                propertiesPath, pluginPath);
        }
        Resource propertyResource = new FileSystemResource(propertiesPath);
        YamlPluginLoader yamlPluginLoader = new YamlPluginLoader(propertyResource);
        return yamlPluginLoader.load();
    }

    protected Path getManifestPath(Path pluginPath, String propertiesFileName) {
        if (Files.isDirectory(pluginPath)) {
            for (String location : PLUGIN_CLASSPATH.getClassesDirectories()) {
                var path = pluginPath.resolve(location).resolve(propertiesFileName);
                Resource propertyResource = new FileSystemResource(path);
                if (propertyResource.exists()) {
                    return path;
                }
            }
            throw new PluginRuntimeException(
                "Unable to find plugin descriptor file: " + DEFAULT_PROPERTIES_FILE_NAME);
        } else {
            // it's a jar file
            try {
                return FileUtils.getPath(pluginPath, propertiesFileName);
            } catch (IOException e) {
                throw new PluginRuntimeException(e);
            }
        }
    }
}
