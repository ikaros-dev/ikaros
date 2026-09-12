package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface OfflineCacheEntryRepository extends ReactiveCrudRepository<OfflineCacheEntryEntity, UUID> {
    Flux<OfflineCacheEntryEntity> findAllByUserIdAndDeviceIdOrderByLastAccessedAtDesc(UUID userId, UUID deviceId);
    Mono<OfflineCacheEntryEntity> findFirstByUserIdAndDeviceIdAndResourceIdAndAttachmentIdAndState(UUID userId, UUID deviceId, UUID resourceId, UUID attachmentId, CacheEntryState state);
    Mono<OfflineCacheEntryEntity> findFirstByUserIdAndDeviceIdAndResourceIdAndAttachmentIdIsNullAndState(UUID userId, UUID deviceId, UUID resourceId, CacheEntryState state);
}
