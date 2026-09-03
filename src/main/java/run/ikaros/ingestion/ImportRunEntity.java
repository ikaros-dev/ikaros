package run.ikaros.ingestion;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.*;
@Table("ingestion_import_run")
public record ImportRunEntity(@Id UUID id, @Column("plan_id") UUID planId, @Column("owner_id") UUID ownerId,
    @Column("actor_id") UUID actorId, String status, String checkpoint, @Column("completed_count") long completedCount,
    @Column("failed_count") long failedCount, @Column("skipped_count") long skippedCount,
    @Column("background_task_id") UUID backgroundTaskId, @Column("started_at") Instant startedAt,
    @Column("finished_at") Instant finishedAt, @Column("created_at") Instant createdAt, @Version Long version) { }
