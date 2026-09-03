package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_delivery_binding")
public record MediaDeliveryBindingEntity(
    @Id UUID id,
    @Column("storage_provider_id") UUID storageProviderId,
    @Column("delivery_provider_key") String deliveryProviderKey,
    @Column("origin_type") DeliveryBindingOriginType originType,
    @Column("auth_mode") DeliveryBindingAuthMode authMode,
    int priority,
    boolean enabled,
    @Column("cache_key_policy") DeliveryBindingCacheKeyPolicy cacheKeyPolicy,
    @Column("range_policy") DeliveryBindingRangePolicy rangePolicy,
    @Column("fallback_participation") boolean fallbackParticipation,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {}
