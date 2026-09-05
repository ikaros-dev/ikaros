package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record MediaDeliveryBindingView(UUID id, UUID storageProviderId, String deliveryProviderKey,
    int priority, boolean enabled,
    DeliveryBindingCacheKeyPolicy cacheKeyPolicy, DeliveryBindingRangePolicy rangePolicy,
    boolean fallbackParticipation, Instant createdAt, Instant updatedAt, Long version) {
    public MediaDeliveryBindingView(UUID id, UUID storageProviderId, String deliveryProviderKey,
        int priority, boolean enabled,
        DeliveryBindingCacheKeyPolicy cacheKeyPolicy, DeliveryBindingRangePolicy rangePolicy,
        boolean fallbackParticipation, Instant createdAt, Instant updatedAt) {
        this(id, storageProviderId, deliveryProviderKey, priority, enabled, cacheKeyPolicy,
            rangePolicy, fallbackParticipation, createdAt, updatedAt, null);
    }
}
