package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_import_plan_item")
public record ImportPlanItemEntity(@Id UUID id, @Column("plan_id") UUID planId,
    @Column("candidate_id") UUID candidateId, String action, @Column("target_id") UUID targetId,
    String reason, int confidence, @Column("idempotency_key") String idempotencyKey,
    @Column("created_at") Instant createdAt, @Version Long version) { }
