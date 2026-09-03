package run.ikaros.backup;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface RestorePointRepository extends ReactiveCrudRepository<RestorePointEntity, UUID> {
    Flux<RestorePointEntity> findAllByOrderByCreatedAtDesc();
}
