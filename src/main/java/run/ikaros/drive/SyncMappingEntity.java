package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("drive_sync_item_mapping")
public record SyncMappingEntity(@Id UUID id, @Column("binding_id") UUID bindingId,
    @Column("local_item_id") String localItemId, @Column("remote_node_id") UUID remoteNodeId,
    @Column("last_synced_revision_id") UUID lastSyncedRevisionId,
    @Column("last_synced_fingerprint") String lastSyncedFingerprint,
    @Column("last_seen_remote_version") long lastSeenRemoteVersion, SyncMappingState state,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
