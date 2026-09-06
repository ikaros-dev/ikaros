package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("offline_download_intent")
public record DownloadIntentEntity(@Id UUID id, @Column("user_id") UUID userId,
    @Column("device_id") UUID deviceId, @Column("resource_id") UUID resourceId,
    @Column("attachment_id") UUID attachmentId, OfflineCopyKind kind, DownloadState state,
    @Column("failure_reason") String failureReason, @Column("manifest_version") long manifestVersion,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
