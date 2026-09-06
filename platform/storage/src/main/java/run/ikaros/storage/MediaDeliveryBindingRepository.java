package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaDeliveryBindingRepository extends ReactiveCrudRepository<MediaDeliveryBindingEntity, UUID> {
    Flux<MediaDeliveryBindingEntity> findAllByStorageProviderIdOrderByPriorityAsc(UUID storageProviderId);
    Mono<MediaDeliveryBindingEntity> findByStorageProviderIdAndDeliveryProviderKey(UUID storageProviderId, String deliveryProviderKey);
    Flux<MediaDeliveryBindingEntity> findAllByDeliveryProviderKeyAndEnabledTrueOrderByPriorityAsc(String deliveryProviderKey);
    Mono<Boolean> existsByDeliveryProviderKey(String deliveryProviderKey);
}
