package run.ikaros.offline;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface OfflineCacheService {
    Mono<CacheEntryView> put(UUID userId, CreateCacheEntryRequest request);
    Flux<CacheEntryView> list(UUID userId, UUID deviceId);
    Mono<CacheEntryView> touch(UUID userId, UUID entryId);
    Mono<Void> evict(UUID userId, UUID entryId);
}
