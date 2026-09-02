package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_delivery_provider")
public record DeliveryProviderEntity(@Id UUID id, @Column("provider_key") String providerKey,
    @Column("provider_type") DeliveryProviderType providerType, @Column("display_name") String displayName,
    @Column("credential_ref") String credentialRef, String config, String capabilities,
    @Column("grant_revocation_mode") DeliveryGrantRevocationLevel grantRevocationMode,
    @Column("signing_key_version") long signingKeyVersion, @Column("health_status") DeliveryProviderHealthStatus healthStatus,
    boolean enabled, @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Version Long version, @Column("idempotency_key") String idempotencyKey) {
    public DeliveryProviderEntity(UUID id, String providerKey, DeliveryProviderType providerType, String displayName,
        String credentialRef, String config, String capabilities, DeliveryGrantRevocationLevel grantRevocationMode,
        long signingKeyVersion, DeliveryProviderHealthStatus healthStatus, boolean enabled, Instant createdAt,
        Instant updatedAt, Long version) {
        this(id, providerKey, providerType, displayName, credentialRef, config, capabilities, grantRevocationMode,
            signingKeyVersion, healthStatus, enabled, createdAt, updatedAt, version, null);
    }
}
