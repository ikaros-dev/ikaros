package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("storage_restore_budget")
public record StorageRestoreBudgetEntity(
    @Id UUID id,
    @Column("max_bytes_per_request") long maxBytesPerRequest,
    @Column("max_items_per_request") int maxItemsPerRequest,
    @Column("max_concurrent_operations") int maxConcurrentOperations,
    @Column("max_concurrent_bytes") long maxConcurrentBytes,
    @Column("daily_requested_bytes") long dailyRequestedBytes,
    @Column("daily_provider_restore_bytes") long dailyProviderRestoreBytes,
    @Column("over_budget_action") StorageRestoreBudgetAction overBudgetAction,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {}
