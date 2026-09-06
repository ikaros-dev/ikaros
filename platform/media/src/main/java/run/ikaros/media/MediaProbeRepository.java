package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MediaProbeRepository extends ReactiveCrudRepository<MediaProbeEntity, UUID> {
    Mono<MediaProbeEntity> findByReleaseIdAndProbeProfileVersion(UUID releaseId, String profileVersion);
}
