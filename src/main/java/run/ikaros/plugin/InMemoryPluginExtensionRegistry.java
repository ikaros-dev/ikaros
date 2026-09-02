package run.ikaros.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** 只保存运行时扩展索引，不允许插件借此获得平台私有持久化访问。 */
@Service
public class InMemoryPluginExtensionRegistry implements PluginExtensionRegistry {
    private final Map<String, List<PluginExtension>> extensions = new ConcurrentHashMap<>();

    @Override
    public void register(PluginManifest manifest) {
        List<PluginExtension> bindings = manifest.extensionPoints().stream()
            .map(point -> new PluginExtension(manifest.pluginId(), point, manifest.entrypoint()))
            .toList();
        extensions.put(manifest.pluginId(), bindings);
    }

    @Override
    public void unregister(String pluginId) { extensions.remove(pluginId); }

    @Override
    public List<PluginExtension> find(String extensionPoint) {
        return extensions.values().stream().flatMap(List::stream)
            .filter(extension -> extension.extensionPoint().equals(extensionPoint)).toList();
    }
}
