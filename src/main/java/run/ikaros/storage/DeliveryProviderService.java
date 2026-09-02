package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeliveryProviderService {
    Mono<DeliveryProviderView> create(DeliveryProviderWriteRequest request);
    Mono<DeliveryProviderView> create(DeliveryProviderWriteRequest request, String idempotencyKey);
    Flux<DeliveryProviderView> list();
    Mono<DeliveryProviderView> get(UUID id);
    Mono<DeliveryProviderView> update(UUID id, DeliveryProviderWriteRequest request, long expectedVersion);
}
