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
import run.ikaros.event.DurableEventService;

@Service
public class PersistentMediaDeliveryBindingService implements MediaDeliveryBindingService {
    private final StorageProviderRegistry providers;
    private final MediaDeliveryBindingRepository bindings;
    private final MediaDeliveryLeaseRepository leases;
    private final DeliveryProviderRepository deliveryProviders;
    private final DurableEventService events;

    public PersistentMediaDeliveryBindingService(StorageProviderRegistry providers, MediaDeliveryBindingRepository bindings,
                                                 MediaDeliveryLeaseRepository leases, DeliveryProviderRepository deliveryProviders,
                                                 DurableEventService events) {
        this.providers = providers;
        this.bindings = bindings;
        this.leases = leases;
        this.deliveryProviders = deliveryProviders;
        this.events = events;
    }

    @Override
    public Mono<MediaDeliveryBindingView> create(UUID providerId, MediaDeliveryBindingRequest request) {
        return providers.get(providerId).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .then(deliveryProviders.findByProviderKey(request.deliveryProviderKey().trim())
                .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
                .flatMap(deliveryProvider -> bindings.findByStorageProviderIdAndDeliveryProviderKey(providerId, request.deliveryProviderKey())
                    .flatMap(old -> Mono.<MediaDeliveryBindingEntity>error(new ConflictException("Delivery Binding 已存在")))
                    .switchIfEmpty(Mono.defer(() -> save(null, providerId, request)))
                    .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("Delivery Binding 已存在"))
                    .flatMap(saved -> events.append("storage.delivery-binding.created", 1, "delivery_binding", saved.id(),
                        "{\"binding_id\":\"" + saved.id() + "\",\"storage_provider_id\":\"" + saved.storageProviderId()
                            + "\",\"delivery_provider_id\":\"" + deliveryProvider.id() + "\",\"priority\":"
                            + saved.priority() + "}").thenReturn(view(saved)))));
    }

    @Override
    public Flux<MediaDeliveryBindingView> list(UUID providerId) {
        return providers.get(providerId).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .thenMany(bindings.findAllByStorageProviderIdOrderByPriorityAsc(providerId).take(100).map(this::view));
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
        return bindings.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Binding 不存在")))
            .flatMap(old -> {
                long actualVersion = old.version() == null ? 0 : old.version();
                if (expectedVersion != null && actualVersion != expectedVersion) {
                    return Mono.error(new PreconditionFailedException("If-Match 与 Delivery Binding 当前版本不匹配"));
                }
                return deliveryProviders.findByProviderKey(request.deliveryProviderKey().trim())
                    .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
                    .then(save(old, old.storageProviderId(), request));
            }).flatMap(saved -> events.append("storage.delivery-binding.updated", 1, "delivery_binding", saved.id(),
                "{\"binding_id\":\"" + saved.id() + "\",\"changed_fields\":[\"configuration\"],\"version\":"
                    + (saved.version() == null ? 0 : saved.version()) + "}").thenReturn(view(saved)));
    }

    @Override
    public Mono<Void> delete(UUID id) {
        return bindings.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Binding 不存在")))
            .flatMap(binding -> leases.existsByBindingId(binding.id()).flatMap(referenced -> {
                if (!referenced) {
                    return bindings.delete(binding)
                        .then(events.append("storage.delivery-binding.removed", 1, "delivery_binding", binding.id(),
                            "{\"binding_id\":\"" + binding.id() + "\"}").then());
                }
                if (!binding.enabled()) return Mono.<Void>empty();
                MediaDeliveryBindingEntity disabled = new MediaDeliveryBindingEntity(binding.id(), binding.storageProviderId(),
                    binding.deliveryProviderKey(), binding.originType(), binding.authMode(), binding.priority(), false,
                    binding.cacheKeyPolicy(), binding.rangePolicy(), binding.fallbackParticipation(), binding.createdAt(),
                    Instant.now(), binding.version());
                return bindings.save(disabled)
                    .flatMap(saved -> events.append("storage.delivery-binding.disabled", 1, "delivery_binding", saved.id(),
                        "{\"binding_id\":\"" + saved.id() + "\",\"reason\":\"referenced_by_delivery_lease\"}").then());
            }));
    }

    private Mono<MediaDeliveryBindingEntity> save(MediaDeliveryBindingEntity old, UUID providerId,
                                                   MediaDeliveryBindingRequest request) {
        Instant now = Instant.now();
        return bindings.save(new MediaDeliveryBindingEntity(old == null ? null : old.id(), providerId,
            request.deliveryProviderKey().trim(), DeliveryBindingOriginType.STORAGE_PROVIDER, DeliveryBindingAuthMode.DELIVERY_GRANT, request.priority(),
            request.enabled(), request.cacheKeyPolicy(), request.rangePolicy(), request.fallbackParticipation(),
            old == null ? now : old.createdAt(), now, old == null ? null : old.version()));
    }

    private MediaDeliveryBindingView view(MediaDeliveryBindingEntity e) {
        return new MediaDeliveryBindingView(e.id(), e.storageProviderId(), e.deliveryProviderKey(), e.priority(), e.enabled(),
            e.cacheKeyPolicy(), e.rangePolicy(), e.fallbackParticipation(),
            e.createdAt(), e.updatedAt(), e.version());
    }

}
