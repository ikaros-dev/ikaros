package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaDeliveryBindingService {
    Mono<MediaDeliveryBindingView> create(UUID storageProviderId, MediaDeliveryBindingRequest request);
    Flux<MediaDeliveryBindingView> list(UUID storageProviderId);
    Mono<MediaDeliveryBindingView> update(UUID id, MediaDeliveryBindingRequest request);
    Mono<MediaDeliveryBindingView> update(UUID id, MediaDeliveryBindingRequest request, long expectedVersion);
    Mono<Void> delete(UUID id);
}
