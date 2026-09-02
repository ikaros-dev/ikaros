package run.ikaros.plugin;

import java.util.Set;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/** 插件公开运行时边界；不提供 Repository、DataSource 或 Spring Bean 访问。 */
public interface PluginRuntime {
    Mono<PluginDescriptor> install(PluginManifest manifest, Set<String> grantedPermissions);
    Mono<PluginDescriptor> enable(String pluginId);
    Mono<PluginDescriptor> disable(String pluginId);
    Mono<Void> uninstall(String pluginId);
    Mono<PluginDescriptor> get(String pluginId);
    Flux<PluginDescriptor> list();
}
