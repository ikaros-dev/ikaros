package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 以内容摘要标识的实际字节内容，不绑定任意业务 Resource 或物理路径。
 */
@Table("blob")
public record BlobEntity(
    @Id UUID id,
    @Column("hash_algorithm") String hashAlgorithm,
    String sha256,
    @Column("size_bytes") long sizeBytes,
    @Column("media_type") String mediaType,
    BlobAvailability availability,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
    public BlobEntity(UUID id, String sha256, long sizeBytes, String mediaType,
                      BlobAvailability availability, Instant createdAt, Long version) {
        this(id, "SHA-256", sha256, sizeBytes, mediaType, availability, createdAt, version);
    }
}
