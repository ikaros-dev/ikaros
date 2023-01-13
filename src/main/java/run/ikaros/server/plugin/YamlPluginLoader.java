package run.ikaros.server.plugin;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.Resource;
import run.ikaros.server.core.custom.Plugin;

/**
 * <p>Process the content in yaml that matches the {@link DocumentMatcher} and convert it to an
 * unstructured list.</p>
 * <p>Multiple resources can be processed at one time.</p>
 * <p>The following specified key must be included before the resource can be processed:
 * <pre>
 *     name
 *     clazz
 *     version
 * </pre>
 * Otherwise, skip it and continue to read the next resource.
 * </p>
 */
public class YamlPluginLoader extends YamlProcessor {

    private static final DocumentMatcher DEFAULT_UNSTRUCTURED_MATCHER = properties -> {
        if (properties.containsKey("name")
            && properties.containsKey("clazz")
            && properties.containsKey("version")) {
            return MatchStatus.FOUND;
        }
        return MatchStatus.NOT_FOUND;
    };

    public YamlPluginLoader(Resource... resources) {
        setResources(resources);
        setDocumentMatchers(DEFAULT_UNSTRUCTURED_MATCHER);
    }

    /**
     * load plugin for map.
     *
     * @return plugin
     */
    public Plugin load() {
        AtomicReference<Plugin> plugin = new AtomicReference<>(new Plugin());
        var objectMapper = JsonMapper.builder().build();
        process((properties, map) -> {
            plugin.set(objectMapper.convertValue(map, Plugin.class));
        });
        return plugin.get();
    }
}
