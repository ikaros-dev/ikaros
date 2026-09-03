package run.ikaros.task;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BackgroundTaskAttemptRepository extends ReactiveCrudRepository<BackgroundTaskAttemptEntity, UUID> {
    Flux<BackgroundTaskAttemptEntity> findAllByTaskIdOrderByAttemptNoAsc(UUID taskId);
    Mono<BackgroundTaskAttemptEntity> findByTaskIdAndAttemptNo(UUID taskId, int attemptNo);
}
