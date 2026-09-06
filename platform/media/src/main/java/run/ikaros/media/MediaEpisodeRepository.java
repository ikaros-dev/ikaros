package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaEpisodeRepository extends ReactiveCrudRepository<MediaEpisodeEntity, UUID> {
    Flux<MediaEpisodeEntity> findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(UUID ownerId, UUID seasonId);
    Flux<MediaEpisodeEntity> findAllByOwnerIdAndSubjectIdOrderByEpisodeNumberAsc(UUID ownerId, UUID subjectId);
}
