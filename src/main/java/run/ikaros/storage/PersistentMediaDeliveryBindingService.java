package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;

@Service
public class PersistentMediaDeliveryBindingService implements MediaDeliveryBindingService {
    private final StorageProviderRegistry providers;
    private final MediaDeliveryBindingRepository bindings;

    public PersistentMediaDeliveryBindingService(StorageProviderRegistry providers, MediaDeliveryBindingRepository bindings) {
        this.providers = providers;
        this.bindings = bindings;
    }

    @Override
    public Mono<MediaDeliveryBindingView> create(UUID providerId, MediaDeliveryBindingRequest request) {
        validate(request);
        return providers.get(providerId).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .then(bindings.findByStorageProviderIdAndDeliveryProviderKey(providerId, request.deliveryProviderKey())
                .flatMap(old -> Mono.<MediaDeliveryBindingEntity>error(new ConflictException("Delivery Binding 已存在")))
                .switchIfEmpty(Mono.defer(() -> save(null, providerId, request))))
            .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("Delivery Binding 已存在"))
            .map(this::view);
    }

    @Override
    public Flux<MediaDeliveryBindingView> list(UUID providerId) {
        return providers.get(providerId).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .thenMany(bindings.findAllByStorageProviderIdOrderByPriorityAsc(providerId).map(this::view));
    }

    @Override
    public Mono<MediaDeliveryBindingView> update(UUID id, MediaDeliveryBindingRequest request) {
        return updateInternal(id, request, null);
    }

    @Override
    public Mono<MediaDeliveryBindingView> update(UUID id, MediaDeliveryBindingRequest request, long expectedVersion) {
        return updateInternal(id, request, expectedVersion);
    }

    private Mono<MediaDeliveryBindingView> updateInternal(UUID id, MediaDeliveryBindingRequest request,
                                                          Long expectedVersion) {
        validate(request);
        return bindings.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Binding 不存在")))
            .flatMap(old -> {
                long actualVersion = old.version() == null ? 0 : old.version();
                if (expectedVersion != null && actualVersion != expectedVersion) {
                    return Mono.error(new PreconditionFailedException("If-Match 与 Delivery Binding 当前版本不匹配"));
                }
                return save(old, old.storageProviderId(), request);
            }).map(this::view);
    }

    @Override
    public Mono<Void> delete(UUID id) {
        return bindings.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Binding 不存在")))
            .flatMap(bindings::delete);
    }

    private Mono<MediaDeliveryBindingEntity> save(MediaDeliveryBindingEntity old, UUID providerId,
                                                   MediaDeliveryBindingRequest request) {
        Instant now = Instant.now();
        return bindings.save(new MediaDeliveryBindingEntity(old == null ? null : old.id(), providerId,
            request.deliveryProviderKey().trim(), request.originType(), request.authMode(), request.priority(),
            request.enabled(), request.cacheKeyPolicy(), request.rangePolicy(), request.fallbackParticipation(),
            old == null ? now : old.createdAt(), now, old == null ? null : old.version()));
    }

    private void validate(MediaDeliveryBindingRequest request) {
        if (request.originType() == DeliveryBindingOriginType.SERVER_PROXY
            && !request.fallbackParticipation()) return;
        if (request.originType() == DeliveryBindingOriginType.SERVER_PROXY
            && request.fallbackParticipation() && request.priority() < 0)
            throw new IllegalArgumentException("Server Proxy Binding 优先级无效");
    }

    private MediaDeliveryBindingView view(MediaDeliveryBindingEntity e) {
        return new MediaDeliveryBindingView(e.id(), e.storageProviderId(), e.deliveryProviderKey(), e.originType(),
            e.authMode(), e.priority(), e.enabled(), e.cacheKeyPolicy(), e.rangePolicy(), e.fallbackParticipation(),
            e.createdAt(), e.updatedAt(), e.version());
    }
}
