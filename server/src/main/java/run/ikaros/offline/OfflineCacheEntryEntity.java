package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("offline_cache_entry")
public record OfflineCacheEntryEntity(@Id UUID id, @Column("user_id") UUID userId,
    @Column("device_id") UUID deviceId, @Column("resource_id") UUID resourceId,
    @Column("attachment_id") UUID attachmentId, @Column("size_bytes") long sizeBytes,
    @Column("content_fingerprint") String contentFingerprint, CacheEntryState state,
    @Column("last_accessed_at") Instant lastAccessedAt, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
