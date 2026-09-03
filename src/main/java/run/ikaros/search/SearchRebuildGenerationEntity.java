package run.ikaros.search;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("search_rebuild_generation")
public record SearchRebuildGenerationEntity(
    @Id String id,
    long generation,
    @Column("updated_at") Instant updatedAt
) { }
