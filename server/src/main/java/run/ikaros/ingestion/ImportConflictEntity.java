package run.ikaros.ingestion;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
@Table("ingestion_import_conflict")
public record ImportConflictEntity(@Id UUID id, @Column("plan_id") UUID planId, @Column("candidate_id") UUID candidateId,
    @Column("owner_id") UUID ownerId, String reason, int confidence, String status, String resolution,
    @Column("created_at") Instant createdAt, @Column("resolved_at") Instant resolvedAt, @Version Long version) { }
