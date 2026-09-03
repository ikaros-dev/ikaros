package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface SyncBindingRepository extends ReactiveCrudRepository<SyncBindingEntity, UUID> {
    Flux<SyncBindingEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    Flux<SyncBindingEntity> findAllByUserIdAndDeviceIdAndEnabledTrue(UUID userId, UUID deviceId);
}
