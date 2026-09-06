package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface OfflineCacheEntryRepository extends ReactiveCrudRepository<OfflineCacheEntryEntity, UUID> {
    Flux<OfflineCacheEntryEntity> findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(UUID userId, UUID deviceId);
}
