package run.ikaros.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

/** P0/P1 单体内插件运行时；持久化生命周期可在 Operations 模块接入。 */
@Service
public class InMemoryPluginRuntime implements PluginRuntime {
    private final Map<String, PluginDescriptor> plugins = new ConcurrentHashMap<>();
    private final String serverVersion;
    private final PluginExtensionRegistry extensionRegistry;

    public InMemoryPluginRuntime(@Value("${ikaros.server.version:2.0.0}") String serverVersion) {
        this(serverVersion, null);
    }

    public InMemoryPluginRuntime(String serverVersion, PluginExtensionRegistry extensionRegistry) {
        this.serverVersion = serverVersion;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Mono<PluginDescriptor> install(PluginManifest manifest, Set<String> grantedPermissions) {
        if (!compatible(manifest)) {
            return Mono.error(new ConflictException("插件与当前 Server 版本不兼容"));
        }
        Set<String> grants = grantedPermissions == null ? Set.of() : Set.copyOf(grantedPermissions);
        if (!manifest.permissions().containsAll(grants)) {
            return Mono.error(new ConflictException("不能授予插件未声明的权限"));
        }
        PluginDescriptor descriptor = new PluginDescriptor(manifest, PluginLifecycle.INSTALLED, grants);
        if (plugins.putIfAbsent(manifest.pluginId(), descriptor) != null) {
            return Mono.error(new ConflictException("插件已安装"));
        }
        return Mono.just(descriptor);
    }

    @Override
    public Mono<PluginDescriptor> enable(String pluginId) {
        return get(pluginId).flatMap(current -> {
            if (current.lifecycle() != PluginLifecycle.INSTALLED
                && current.lifecycle() != PluginLifecycle.DISABLED) {
                return Mono.error(new ConflictException("插件当前状态不允许启用"));
            }
            PluginDescriptor updated = new PluginDescriptor(current.manifest(), PluginLifecycle.ENABLED,
                current.grantedPermissions());
            plugins.replace(pluginId, current, updated);
            register(updated.manifest());
            return Mono.just(updated);
        });
    }

    @Override
    public Mono<PluginDescriptor> disable(String pluginId) {
        return update(pluginId, PluginLifecycle.ENABLED, PluginLifecycle.DISABLED);
    }

    @Override
    public Mono<Void> uninstall(String pluginId) {
        return Mono.defer(() -> {
            PluginDescriptor current = plugins.get(pluginId);
            if (current == null) return Mono.error(new NotFoundException("插件不存在"));
            if (current.lifecycle() == PluginLifecycle.ENABLED) {
                return Mono.error(new ConflictException("启用中的插件必须先禁用"));
            }
            plugins.remove(pluginId);
            unregister(pluginId);
            return Mono.empty();
        });
    }

    @Override
    public Mono<PluginDescriptor> get(String pluginId) {
        return Mono.justOrEmpty(plugins.get(pluginId))
            .switchIfEmpty(Mono.error(new NotFoundException("插件不存在")));
    }

    @Override
    public Flux<PluginDescriptor> list() {
        return Flux.fromIterable(List.copyOf(plugins.values()));
    }

    private Mono<PluginDescriptor> update(String id, PluginLifecycle expected, PluginLifecycle next) {
        return get(id).flatMap(current -> {
            if (current.lifecycle() != expected) {
                return Mono.error(new ConflictException("插件当前状态不允许执行该操作"));
            }
            PluginDescriptor updated = new PluginDescriptor(current.manifest(), next, current.grantedPermissions());
            plugins.replace(id, current, updated);
            if (next == PluginLifecycle.DISABLED) unregister(id);
            return Mono.just(updated);
        });
    }

    private void register(PluginManifest manifest) {
        if (extensionRegistry != null) extensionRegistry.register(manifest);
    }

    private void unregister(String pluginId) {
        if (extensionRegistry != null) extensionRegistry.unregister(pluginId);
    }

    private boolean compatible(PluginManifest manifest) {
        return serverVersion.compareTo(manifest.minimumServerVersion()) >= 0
            && (manifest.maximumServerVersion() == null
                || serverVersion.compareTo(manifest.maximumServerVersion()) <= 0);
    }
}
