package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaPlaybackSessionRepository extends ReactiveCrudRepository<MediaPlaybackSessionEntity, UUID> {
    Flux<MediaPlaybackSessionEntity> findAllByOwnerIdAndResourceIdOrderByStartedAtDesc(UUID ownerId, UUID resourceId);
}
