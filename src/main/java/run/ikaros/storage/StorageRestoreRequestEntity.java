package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_restore_request")
public record StorageRestoreRequestEntity(@Id UUID id, @Column("actor_id") UUID actorId,
    StorageRestoreScope scope, @Column("scope_id") UUID scopeId, StorageRestoreRequestStatus status,
    @Column("total_items") int totalItems, @Column("completed_items") int completedItems,
    @Column("total_bytes") long totalBytes, @Column("error_summary") String errorSummary,
    @Column("idempotency_key") String idempotencyKey, @Column("background_task_id") UUID backgroundTaskId,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
