package run.ikaros.ingestion;
import java.time.Instant; import java.util.UUID; import run.ikaros.resource.api.MetadataSource;
public record MetadataCandidateView(UUID id,UUID resourceId,String fieldKey,String value,MetadataSource source,
 String sourceReference,int confidence,MetadataCandidateStatus status,Instant createdAt,Instant resolvedAt) { }
