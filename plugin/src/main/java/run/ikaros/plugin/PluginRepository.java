package run.ikaros.plugin;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PluginRepository extends ReactiveCrudRepository<PluginEntity, UUID> {
    Mono<PluginEntity> findByPluginId(String pluginId);
}
