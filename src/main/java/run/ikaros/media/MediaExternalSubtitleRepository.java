package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaExternalSubtitleRepository extends ReactiveCrudRepository<MediaExternalSubtitleEntity, UUID> {
    Flux<MediaExternalSubtitleEntity> findAllByReleaseIdOrderByLanguageAsc(UUID releaseId);
}
