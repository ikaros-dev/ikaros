package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_provider")
public record StorageProviderEntity(@Id UUID id, @Column("provider_key") String providerKey,
                                    @Column("provider_type") String providerType, String tier,
                                    String status, @Column("secret_reference") String secretReference,
                                    @Column("provider_metadata") String providerMetadata,
                                    @Column("access_key_id_ciphertext") String accessKeyIdCiphertext,
                                    @Column("secret_access_key_ciphertext") String secretAccessKeyCiphertext,
                                    @Column("session_token_ciphertext") String sessionTokenCiphertext,
                                    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt) {
    public StorageProviderEntity(UUID id, String providerKey, String providerType, String tier, String status,
                                 String secretReference, String providerMetadata, Instant createdAt, Instant updatedAt) {
        this(id, providerKey, providerType, tier, status, secretReference, providerMetadata, null, null, null,
            createdAt, updatedAt);
    }
}
