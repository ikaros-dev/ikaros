package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaPlaybackHistoryRepository extends ReactiveCrudRepository<MediaPlaybackHistoryEntity, UUID> {
    Flux<MediaPlaybackHistoryEntity> findAllByOwnerIdOrderByEndedAtDesc(UUID ownerId);
}
