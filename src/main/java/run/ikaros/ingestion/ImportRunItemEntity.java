package run.ikaros.ingestion;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.*; import org.springframework.data.relational.core.mapping.*;
@Table("ingestion_import_run_item")
public record ImportRunItemEntity(@Id UUID id,@Column("run_id") UUID runId,@Column("plan_item_id") UUID planItemId,
 String status,@Column("attempt_count") int attemptCount,@Column("error_message") String errorMessage,
 @Column("idempotency_key") String idempotencyKey,@Column("updated_at") Instant updatedAt,@Version Long version) { }
