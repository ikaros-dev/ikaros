package run.ikaros.task;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface BackgroundTaskAttemptRepository extends ReactiveCrudRepository<BackgroundTaskAttemptEntity, UUID> {
}
