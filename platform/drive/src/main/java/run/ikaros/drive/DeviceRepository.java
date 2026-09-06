package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface DeviceRepository extends ReactiveCrudRepository<DeviceEntity, UUID> {
    Flux<DeviceEntity> findAllByUserIdOrderByRegisteredAtAsc(UUID userId);
    Mono<DeviceEntity> findByUserIdAndInstallationId(UUID userId, String installationId);
}
