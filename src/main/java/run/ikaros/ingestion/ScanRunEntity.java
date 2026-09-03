package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_scan_run")
public record ScanRunEntity(
    @Id UUID id,
    @Column("source_id") UUID sourceId,
    @Column("owner_id") UUID ownerId,
    String trigger,
    @Column("actor_id") UUID actorId,
    String status,
    String checkpoint,
    @Column("discovered_count") long discoveredCount,
    @Column("changed_count") long changedCount,
    @Column("skipped_count") long skippedCount,
    @Column("error_summary") String errorSummary,
    @Column("background_task_id") UUID backgroundTaskId,
    @Column("started_at") Instant startedAt,
    @Column("finished_at") Instant finishedAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) { }
