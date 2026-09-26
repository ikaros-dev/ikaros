package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_provider")
public record StorageProviderEntity(@Id UUID id, @Column("provider_key") String providerKey,
                                    @Column("provider_type") String providerType, String tier,
                                    String status, @Column("secret_reference") String secretReference,
                                    @Column("provider_metadata") Json providerMetadata,
                                    @Column("access_key_id_ciphertext") String accessKeyIdCiphertext,
                                    @Column("secret_access_key_ciphertext") String secretAccessKeyCiphertext,
                                    @Column("session_token_ciphertext") String sessionTokenCiphertext,
                                    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
                                    @Column("display_name") String displayName,
                                    @Column("capabilities") Json capabilities,
                                    @Column("enabled") Boolean enabled,
                                    @Column("drain_status") String drainStatus,
                                    @Version Long version,
                                    @Column("configuration") Json configuration) {
    public StorageProviderEntity(UUID id, String providerKey, String providerType, String tier, String status,
                                 String secretReference, String providerMetadata, Instant createdAt, Instant updatedAt) {
        this(id, providerKey, providerType, tier, status, secretReference,
            Json.of(providerMetadata == null ? "{}" : providerMetadata), null, null, null, createdAt, updatedAt,
            providerKey, Json.of("{}"), writable(status), drain(status), null,
            Json.of(providerMetadata == null ? "{}" : providerMetadata));
    }

    public StorageProviderEntity(UUID id, String providerKey, String providerType, String tier, String status,
                                 String secretReference, String providerMetadata, String accessKeyIdCiphertext,
                                 String secretAccessKeyCiphertext, String sessionTokenCiphertext,
                                 Instant createdAt, Instant updatedAt) {
        this(id, providerKey, providerType, tier, status, secretReference,
            Json.of(providerMetadata == null ? "{}" : providerMetadata), accessKeyIdCiphertext,
            secretAccessKeyCiphertext, sessionTokenCiphertext, createdAt, updatedAt, providerKey,
            Json.of("{}"), writable(status), drain(status), null,
            Json.of(providerMetadata == null ? "{}" : providerMetadata));
    }

    public StorageProviderEntity(UUID id, String providerKey, String providerType, String tier, String status,
        String secretReference, String providerMetadata, String accessKeyIdCiphertext, String secretAccessKeyCiphertext,
        String sessionTokenCiphertext, Instant createdAt, Instant updatedAt, String displayName, String capabilities,
        Boolean enabled, String drainStatus, Long version, String configuration) {
        this(id, providerKey, providerType, tier, status, secretReference,
            Json.of(providerMetadata == null ? "{}" : providerMetadata), accessKeyIdCiphertext, secretAccessKeyCiphertext,
            sessionTokenCiphertext, createdAt, updatedAt, displayName, Json.of(capabilities == null ? "{}" : capabilities),
            enabled, drainStatus, version, Json.of(configuration == null ? "{}" : configuration));
    }

    private static Boolean writable(String status) {
        return !"DISABLED".equals(status) && !"DRAINING".equals(status);
    }

    private static String drain(String status) {
        return "DRAINING".equals(status) ? "DRAINING" : "NORMAL";
    }
}
