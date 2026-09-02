package run.ikaros.task;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BackgroundTaskAttemptRepository extends ReactiveCrudRepository<BackgroundTaskAttemptEntity, UUID> {
    Flux<BackgroundTaskAttemptEntity> findAllByTaskIdOrderByAttemptNoAsc(UUID taskId);
}
