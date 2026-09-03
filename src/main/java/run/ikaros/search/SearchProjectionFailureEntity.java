package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("search_projection_failure")
public record SearchProjectionFailureEntity(
    @Id UUID id,
    @Column("source_id") UUID sourceId,
    @Column("source_version") long sourceVersion,
    @Column("rebuild_generation") long rebuildGeneration,
    String reason,
    @Column("failed_at") Instant failedAt,
    @Column("resolved_at") Instant resolvedAt
) { }
