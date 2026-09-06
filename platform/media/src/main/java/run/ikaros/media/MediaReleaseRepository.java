package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaReleaseRepository extends ReactiveCrudRepository<MediaReleaseEntity, UUID> {
    Flux<MediaReleaseEntity> findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(UUID ownerId, UUID resourceId);
}
