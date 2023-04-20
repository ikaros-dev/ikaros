package run.ikaros.server.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.AbstractExtensionFinder;
import org.pf4j.Extension;
import org.pf4j.ExtensionWrapper;
import org.pf4j.PluginWrapper;

@Slf4j
public class IkarosExtensionFinder extends AbstractExtensionFinder {

    private final IkarosPluginManager ikarosPluginManager;

    public IkarosExtensionFinder(IkarosPluginManager pluginManager) {
        super(pluginManager);
        this.ikarosPluginManager = pluginManager;
    }

    @Override
    public <T> List<ExtensionWrapper<T>> find(Class<T> type) {
        log.debug("Finding extensions of extension point '{}'", type.getName());
        if (entries == null) {
            entries = new HashMap<>();
        }
        entries.putAll(readPluginsStorages());

        List<ExtensionWrapper<T>> result = new ArrayList<>();

        // add extensions found in classpath and plugins
        for (String pluginId : entries.keySet()) {
            // classpath's extensions <=> pluginId = null
            List<ExtensionWrapper<T>> pluginExtensions = find(type, pluginId);
            result.addAll(pluginExtensions);
        }

        if (result.isEmpty()) {
            log.debug("No extensions found for extension point '{}'", type.getName());
        } else {
            log.debug("Found {} extensions for extension point '{}'", result.size(),
                type.getName());
        }

        // sort by "ordinal" property
        Collections.sort(result);

        return result;
    }

    @Override
    public Map<String, Set<String>> readPluginsStorages() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (PluginWrapper plugin : ikarosPluginManager.getPlugins()) {
            String pluginId = plugin.getPluginId();
            if (PluginApplicationContextRegistry.getInstance().containsContext(pluginId)) {
                PluginApplicationContext pluginApplicationContext =
                    PluginApplicationContextRegistry.getInstance().getByPluginId(pluginId);
                Map<String, Object> beansWithAnnotation =
                    pluginApplicationContext.getBeansWithAnnotation(Extension.class);
                Set<String> bucket = beansWithAnnotation.values().stream()
                    .flatMap(obj -> Stream.of(obj.getClass().getName()))
                    .collect(Collectors.toSet());

                debugExtensions(bucket);

                result.put(pluginId, bucket);
            }
        }
        return result;
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        return Map.of();
    }

}
