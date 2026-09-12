package run.ikaros.offline;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OfflineCacheQuotaRepository extends ReactiveCrudRepository<OfflineCacheQuotaEntity, UUID> {
    Mono<OfflineCacheQuotaEntity> findByUserIdAndDeviceId(UUID userId, UUID deviceId);
}
