package run.ikaros.ingestion;
import java.time.Instant; import java.util.List; import java.util.UUID; import org.springframework.stereotype.Service; import reactor.core.publisher.Mono;
import run.ikaros.resource.api.MetadataSource; import run.ikaros.resource.api.ResourceOwnershipQuery;
@Service public class DefaultMetadataCandidateService implements MetadataCandidateService {
 private static final int MAX_UNPAGED_RESULTS = 100;
 private final ResourceOwnershipQuery resources; private final MetadataCandidateRepository candidates;
 public DefaultMetadataCandidateService(ResourceOwnershipQuery resources,MetadataCandidateRepository candidates){this.resources=resources;this.candidates=candidates;}
 public Mono<MetadataCandidateView> submit(UUID owner,UUID resource,SubmitMetadataCandidateRequest r){return owned(owner,resource).then(candidates.save(new MetadataCandidateEntity(null,resource,r.fieldKey(),r.value(),r.source().name(),r.sourceReference(),r.confidence(),MetadataCandidateStatus.PENDING.name(),Instant.now(),null,null))).map(this::view);}
 public Mono<List<MetadataCandidateView>> list(UUID owner,UUID resource){return owned(owner,resource).thenMany(candidates.findAllByResourceIdOrderByCreatedAtDesc(resource).take(MAX_UNPAGED_RESULTS)).map(this::view).collectList();}
 private Mono<Void> owned(UUID owner,UUID resource){return resources.requireOwned(owner,resource);}
 private MetadataCandidateView view(MetadataCandidateEntity c){return new MetadataCandidateView(c.id(),c.resourceId(),c.fieldKey(),c.fieldValue(),MetadataSource.valueOf(c.source()),c.sourceReference(),c.confidence(),MetadataCandidateStatus.valueOf(c.status()),c.createdAt(),c.resolvedAt());}
}
