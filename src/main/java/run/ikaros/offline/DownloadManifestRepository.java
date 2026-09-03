package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
public interface DownloadManifestRepository extends ReactiveCrudRepository<DownloadManifestEntity, UUID> {
    Mono<DownloadManifestEntity> findTopByIntentIdOrderByManifestVersionDesc(UUID intentId);
}
