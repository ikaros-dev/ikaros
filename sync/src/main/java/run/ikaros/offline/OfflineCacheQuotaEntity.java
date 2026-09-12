package run.ikaros.offline;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("offline_cache_quota")
public record OfflineCacheQuotaEntity(@Id UUID id, @Column("user_id") UUID userId,
    @Column("device_id") UUID deviceId, @Column("quota_bytes") long quotaBytes,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Version Long version) {}
