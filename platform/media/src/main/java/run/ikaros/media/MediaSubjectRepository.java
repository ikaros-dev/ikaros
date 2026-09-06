package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaSubjectRepository extends ReactiveCrudRepository<MediaSubjectEntity, UUID> {
    Mono<MediaSubjectEntity> findByOwnerIdAndResourceId(UUID ownerId, UUID resourceId);
    Flux<MediaSubjectEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
