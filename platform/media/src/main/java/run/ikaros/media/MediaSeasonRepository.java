package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MediaSeasonRepository extends ReactiveCrudRepository<MediaSeasonEntity, UUID> {
    Flux<MediaSeasonEntity> findAllByOwnerIdAndSubjectIdOrderBySeasonNumberAsc(UUID ownerId, UUID subjectId);
}
