package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MetadataSyncStatusRepository extends ReactiveCrudRepository<MetadataSyncStatusEntity, UUID> {
    Flux<MetadataSyncStatusEntity> findTop50ByOwnerIdAndSyncSourceIdOrderByCheckedAtDesc(UUID ownerId, UUID syncSourceId);
}
