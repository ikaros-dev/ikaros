package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_sync_conflict")
public record SyncConflictEntity(@Id UUID id, @Column("binding_id") UUID bindingId, @Column("node_id") UUID nodeId,
    @Column("base_revision_id") UUID baseRevisionId, @Column("remote_revision_id") UUID remoteRevisionId,
    @Column("local_fingerprint") String localFingerprint, SyncConflictState state,
    @Column("detected_at") Instant detectedAt, @Column("resolved_at") Instant resolvedAt,
    @Column("resolved_by") UUID resolvedBy, @Version Long version) {}
