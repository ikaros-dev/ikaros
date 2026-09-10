package run.ikaros.plugin;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {
    private final PluginRuntime runtime;

    public PluginController(PluginRuntime runtime) { this.runtime = runtime; }

    @GetMapping
    public Flux<PluginDescriptor> list(@RequestHeader("X-Ikaros-Actor-Id") String actorId) {
        return runtime.list();
    }

    @PostMapping
    public Mono<ResponseEntity<PluginDescriptor>> install(
        @RequestHeader("X-Ikaros-Actor-Id") String actorId,
        @Valid @RequestBody InstallPluginRequest request
    ) {
        return runtime.install(request.manifest(), request.grantedPermissions())
            .map(plugin -> ResponseEntity.created(URI.create("/api/plugins/" + plugin.manifest().pluginId())).body(plugin));
    }

    @GetMapping("/{pluginId}")
    public Mono<PluginDescriptor> get(@RequestHeader("X-Ikaros-Actor-Id") String actorId,
                                      @PathVariable String pluginId) {
        return runtime.get(pluginId);
    }

    @PostMapping("/{pluginId}/enable")
    public Mono<PluginDescriptor> enable(@RequestHeader("X-Ikaros-Actor-Id") String actorId,
                                         @PathVariable String pluginId) {
        return runtime.enable(pluginId);
    }

    @PostMapping("/{pluginId}/disable")
    public Mono<PluginDescriptor> disable(@RequestHeader("X-Ikaros-Actor-Id") String actorId,
                                          @PathVariable String pluginId) {
        return runtime.disable(pluginId);
    }

    @PostMapping("/{pluginId}/upgrade")
    public Mono<PluginDescriptor> upgrade(@RequestHeader("X-Ikaros-Actor-Id") String actorId,
                                          @PathVariable String pluginId,
                                          @Valid @RequestBody UpgradePluginRequest request) {
        return runtime.upgrade(pluginId, request.manifest(), request.grantedPermissions());
    }

    @PostMapping("/{pluginId}/uninstall")
    public Mono<ResponseEntity<Void>> uninstall(@RequestHeader("X-Ikaros-Actor-Id") String actorId,
                                                @PathVariable String pluginId,
                                                @Valid @RequestBody UninstallPluginRequest request) {
        return runtime.uninstall(pluginId, request.retention()).thenReturn(ResponseEntity.noContent().build());
    }
}
