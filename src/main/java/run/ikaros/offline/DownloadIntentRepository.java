package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface DownloadIntentRepository extends ReactiveCrudRepository<DownloadIntentEntity, UUID> {
    Flux<DownloadIntentEntity> findAllByUserIdAndDeviceIdOrderByCreatedAtDesc(UUID userId, UUID deviceId);
}
