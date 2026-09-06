package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaTrackRepository extends ReactiveCrudRepository<MediaTrackEntity, UUID> {
    Flux<MediaTrackEntity> findAllByProbeIdOrderByStableKeyAsc(UUID probeId);
}
