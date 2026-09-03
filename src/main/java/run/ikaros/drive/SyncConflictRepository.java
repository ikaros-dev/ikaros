package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface SyncConflictRepository extends ReactiveCrudRepository<SyncConflictEntity, UUID> {
    Flux<SyncConflictEntity> findAllByBindingIdOrderByDetectedAtDesc(UUID bindingId);
}
