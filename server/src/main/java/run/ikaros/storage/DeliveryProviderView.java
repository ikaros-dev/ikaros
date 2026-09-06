package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DeliveryProviderView(UUID id, String providerKey, DeliveryProviderType providerType, String displayName,
    String credentialRef, Map<String, Object> config, Map<String, Object> capabilities,
    DeliveryGrantRevocationLevel grantRevocationMode, long signingKeyVersion,
    DeliveryProviderHealthStatus healthStatus, boolean enabled, Instant createdAt, Instant updatedAt, long version) {}
