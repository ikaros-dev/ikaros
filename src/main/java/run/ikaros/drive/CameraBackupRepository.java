package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface CameraBackupRepository extends ReactiveCrudRepository<CameraBackupEntity, UUID> {
    Mono<CameraBackupEntity> findByBindingIdAndSourceItemId(UUID bindingId, String sourceItemId);
    Flux<CameraBackupEntity> findAllByBindingIdOrderByUpdatedAtAsc(UUID bindingId);
}
