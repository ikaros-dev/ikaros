package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ReadingBookmarkRepository extends ReactiveCrudRepository<ReadingBookmarkEntity, UUID> {
    Flux<ReadingBookmarkEntity> findAllByOwnerIdAndWorkIdOrderByCreatedAtDesc(UUID ownerId, UUID workId);
}
