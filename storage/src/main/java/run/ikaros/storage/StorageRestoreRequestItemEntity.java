package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_restore_request_item")
public record StorageRestoreRequestItemEntity(@Id UUID id, @Column("request_id") UUID requestId,
    @Column("placement_id") UUID placementId, @Column("operation_id") UUID operationId,
    StorageRestoreRequestItemStatus status, @Column("error_summary") String errorSummary,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
