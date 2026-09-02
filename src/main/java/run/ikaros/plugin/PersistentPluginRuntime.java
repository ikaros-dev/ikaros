package run.ikaros.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import reactor.core.publisher.Flux;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

/** 基于 plugin 表的默认生命周期实现。 */
@Primary
@Service
public class PersistentPluginRuntime implements PluginRuntime {
    private final PluginRepository repository;
    private final ObjectMapper mapper;
    private final PluginExtensionRegistry extensionRegistry;
    private final String serverVersion = "2.0.0";

    public PersistentPluginRuntime(PluginRepository repository, ObjectMapper mapper) {
        this(repository, mapper, null);
    }

    @Autowired
    public PersistentPluginRuntime(PluginRepository repository, ObjectMapper mapper,
                                   PluginExtensionRegistry extensionRegistry) {
        this.repository = repository;
        this.mapper = mapper;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Mono<PluginDescriptor> install(PluginManifest manifest, Set<String> grantedPermissions) {
        Set<String> grants = grantedPermissions == null ? Set.of() : Set.copyOf(grantedPermissions);
        if (!manifest.permissions().containsAll(grants)) {
            return Mono.error(new ConflictException("不能授予插件未声明的权限"));
        }
        return repository.findByPluginId(manifest.pluginId()).flatMap(existing ->
            Mono.<PluginDescriptor>error(new ConflictException("插件已安装")))
            .switchIfEmpty(Mono.defer(() -> {
                Instant now = Instant.now();
                return encode(manifest, grants).flatMap(json -> repository.save(new PluginEntity(null,
                    manifest.pluginId(), json.manifest(), PluginLifecycle.INSTALLED.name(), json.permissions(), now, now)))
                    .map(saved -> new PluginDescriptor(manifest, PluginLifecycle.INSTALLED, grants));
            }));
    }

    @Override
    public Mono<PluginDescriptor> enable(String pluginId) { return change(pluginId, PluginLifecycle.ENABLED); }

    @Override
    public Mono<PluginDescriptor> disable(String pluginId) { return change(pluginId, PluginLifecycle.DISABLED); }

    @Override
    public Mono<Void> uninstall(String pluginId) {
        return repository.findByPluginId(pluginId).switchIfEmpty(Mono.error(new NotFoundException("插件不存在")))
            .flatMap(entity -> PluginLifecycle.ENABLED.name().equals(entity.status())
                ? Mono.error(new ConflictException("启用中的插件必须先禁用")) : repository.delete(entity)
                    .doOnSuccess(ignored -> unregister(pluginId)));
    }

    @Override
    public Mono<PluginDescriptor> get(String pluginId) {
        return repository.findByPluginId(pluginId).switchIfEmpty(Mono.error(new NotFoundException("插件不存在")))
            .flatMap(this::descriptor);
    }

    @Override
    public Flux<PluginDescriptor> list() {
        return repository.findAll().flatMap(this::descriptor);
    }

    private Mono<PluginDescriptor> change(String pluginId, PluginLifecycle next) {
        return repository.findByPluginId(pluginId).switchIfEmpty(Mono.error(new NotFoundException("插件不存在")))
            .flatMap(entity -> descriptor(entity).flatMap(current -> {
                boolean allowed = next == PluginLifecycle.ENABLED
                    ? current.lifecycle() == PluginLifecycle.INSTALLED || current.lifecycle() == PluginLifecycle.DISABLED
                    : current.lifecycle() == PluginLifecycle.ENABLED;
                if (!allowed) return Mono.error(new ConflictException("插件当前状态不允许执行该操作"));
                PluginEntity updated = new PluginEntity(entity.id(), entity.pluginId(), entity.manifestJson(),
                    next.name(), entity.grantedPermissionsJson(), entity.createdAt(), Instant.now());
                return repository.save(updated).map(saved -> {
                    if (next == PluginLifecycle.ENABLED) {
                        register(current.manifest());
                    } else {
                        unregister(pluginId);
                    }
                    return new PluginDescriptor(current.manifest(), next, current.grantedPermissions());
                });
            }));
    }

    private void register(PluginManifest manifest) {
        if (extensionRegistry != null) extensionRegistry.register(manifest);
    }

    private void unregister(String pluginId) {
        if (extensionRegistry != null) extensionRegistry.unregister(pluginId);
    }

    private Mono<PluginDescriptor> descriptor(PluginEntity entity) {
        try {
            PluginManifest manifest = mapper.readValue(entity.manifestJson(), PluginManifest.class);
            Set<String> grants = mapper.readValue(entity.grantedPermissionsJson(), mapper.getTypeFactory()
                .constructCollectionType(Set.class, String.class));
            return Mono.just(new PluginDescriptor(manifest, PluginLifecycle.valueOf(entity.status()), grants));
        } catch (JsonProcessingException | IllegalArgumentException error) {
            return Mono.error(new ConflictException("插件 Manifest 数据损坏"));
        }
    }

    private Mono<Encoded> encode(PluginManifest manifest, Set<String> permissions) {
        try {
            return Mono.just(new Encoded(mapper.writeValueAsString(manifest), mapper.writeValueAsString(permissions)));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalArgumentException("插件 Manifest 无法序列化", error));
        }
    }

    private record Encoded(String manifest, String permissions) { }
}
