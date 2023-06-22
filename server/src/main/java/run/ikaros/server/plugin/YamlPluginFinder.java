package run.ikaros.server.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.infra.utils.JsonUtils;

/**
 * Reading plugin descriptor data from plugin.yaml.
 *
 * @see Plugin
 */
@Slf4j
public class YamlPluginFinder {
    static final DevelopmentPluginClasspath PLUGIN_CLASSPATH = new DevelopmentPluginClasspath();
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "plugin.yaml";
    public static final String DEFAULT_PLUG_CONFIG_MAG_SCHEMAS_FILE_NAME = "configMapSchemas.yaml";
    private final String propertiesFileName;
    private final IkarosPluginManager pluginManager;

    public YamlPluginFinder(IkarosPluginManager pluginManager) {
        this(DEFAULT_PROPERTIES_FILE_NAME, pluginManager);
    }

    public YamlPluginFinder(String propertiesFileName, IkarosPluginManager pluginManager) {
        this.propertiesFileName = propertiesFileName;
        this.pluginManager = pluginManager;
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
        Plugin plugin = yamlPluginLoader.load();

        // load configMapSchemas
        Path configMapSchemasPath =
            getManifestPath(pluginPath, DEFAULT_PLUG_CONFIG_MAG_SCHEMAS_FILE_NAME);
        if (configMapSchemasPath == null || Files.notExists(configMapSchemasPath)) {
            log.warn("Cannot find '{}' in resource path for plugin: '{}'",
                configMapSchemasPath, pluginPath);
            return plugin;
        }

        try {
            // 使用ObjectMapper将YAML文件加载为Java对象
            Object yamlObject =
                new ObjectMapper(new YAMLFactory()).readValue(configMapSchemasPath.toFile(),
                    Object.class);
            // 将Java对象转换为JSON字符串
            plugin.setConfigMapSchemas(JsonUtils.obj2Json(yamlObject));
        } catch (IOException e) {
            throw new PluginRuntimeException("Cannot read '{}' in resource path for plugin: '{}'",
                configMapSchemasPath, pluginPath);
        }

        return plugin;
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
            if (DEFAULT_PROPERTIES_FILE_NAME.equalsIgnoreCase(propertiesFileName)) {
                throw new PluginRuntimeException(
                    "Unable to find plugin descriptor file: " + propertiesFileName);
            } else {
                return null;
            }
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
