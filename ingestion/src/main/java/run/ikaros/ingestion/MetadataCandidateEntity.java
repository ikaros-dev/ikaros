package run.ikaros.ingestion;
import java.time.Instant; import java.util.UUID;
import org.springframework.data.annotation.*; import org.springframework.data.relational.core.mapping.*;
@Table("ingestion_metadata_candidate")
public record MetadataCandidateEntity(@Id UUID id,@Column("resource_id") UUID resourceId,
 @Column("field_key") String fieldKey,@Column("field_value") String fieldValue,String source,
 @Column("source_reference") String sourceReference,int confidence,String status,
 @Column("created_at") Instant createdAt,@Column("resolved_at") Instant resolvedAt,@Version Long version) { }
