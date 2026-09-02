package run.ikaros.task;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface BackgroundTaskRepository extends ReactiveCrudRepository<BackgroundTaskEntity, UUID> {
    Mono<BackgroundTaskEntity> findByTaskTypeAndIdempotencyKey(String taskType, String idempotencyKey);
    Mono<BackgroundTaskEntity> findTop1ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscCreatedAtAsc(
        String status, Instant now);
}
