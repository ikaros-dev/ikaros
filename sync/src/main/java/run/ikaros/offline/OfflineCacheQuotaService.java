package run.ikaros.offline;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface OfflineCacheQuotaService {
    Mono<OfflineCacheQuotaView> get(UUID userId, UUID deviceId);
    Mono<OfflineCacheQuotaView> set(UUID userId, SetOfflineCacheQuotaRequest request);
}
