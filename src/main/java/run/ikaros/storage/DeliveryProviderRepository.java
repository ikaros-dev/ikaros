package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DeliveryProviderRepository extends ReactiveCrudRepository<DeliveryProviderEntity, UUID> {
    Mono<DeliveryProviderEntity> findByProviderKey(String providerKey);
    Mono<DeliveryProviderEntity> findByIdempotencyKey(String idempotencyKey);
}
