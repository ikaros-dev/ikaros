package run.ikaros.search;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("search_document")
public record SearchDocumentEntity(@Id UUID documentId, @Column("source_id") UUID sourceId,
                                   @Column("source_version") long sourceVersion,
                                   @Column("projector_version") String projectorVersion,
                                   @Column("rebuild_generation") long rebuildGeneration,
                                   @Column("fields_json") String fieldsJson,
                                   @Column("projected_at") Instant projectedAt) {
}
