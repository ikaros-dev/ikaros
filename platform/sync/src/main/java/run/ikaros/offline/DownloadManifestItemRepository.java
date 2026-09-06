package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface DownloadManifestItemRepository extends ReactiveCrudRepository<DownloadManifestItemEntity, UUID> {
    Flux<DownloadManifestItemEntity> findAllByManifestId(UUID manifestId);
}
