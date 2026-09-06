package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_restore_operation")
public record StorageRestoreOperationEntity(@Id UUID id, @Column("placement_id") UUID placementId,
    @Column("provider_restore_class") String providerRestoreClass, @Column("restore_generation") long restoreGeneration,
    @Column("operation_key") String operationKey,
    StorageRestoreOperationStatus status, @Column("background_task_id") UUID backgroundTaskId,
    @Column("provider_operation_id") String providerOperationId, @Column("restore_expires_at") Instant restoreExpiresAt,
    @Column("error_summary") String errorSummary, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {
    /** Compatibility constructor for pre-operation-key callers. */
    public StorageRestoreOperationEntity(UUID id, UUID placementId, String providerRestoreClass,
        long restoreGeneration, StorageRestoreOperationStatus status, UUID backgroundTaskId,
        String providerOperationId, Instant restoreExpiresAt, String errorSummary,
        Instant createdAt, Instant updatedAt, Long version) {
        this(id, placementId, providerRestoreClass, restoreGeneration,
            id == null ? null : placementId + ":" + providerRestoreClass + ":" + restoreGeneration,
            status, backgroundTaskId, providerOperationId, restoreExpiresAt, errorSummary,
            createdAt, updatedAt, version);
    }
}
