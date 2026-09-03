package run.ikaros.ingestion;
import java.time.Instant; import java.util.List; import java.util.UUID; import org.springframework.stereotype.Service; import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException; import run.ikaros.resource.ResourceRepository;
@Service public class DefaultMetadataCandidateService implements MetadataCandidateService {
 private static final int MAX_UNPAGED_RESULTS = 100;
 private final ResourceRepository resources; private final MetadataCandidateRepository candidates;
 public DefaultMetadataCandidateService(ResourceRepository resources,MetadataCandidateRepository candidates){this.resources=resources;this.candidates=candidates;}
 public Mono<MetadataCandidateView> submit(UUID owner,UUID resource,SubmitMetadataCandidateRequest r){return owned(owner,resource).then(candidates.save(new MetadataCandidateEntity(null,resource,r.fieldKey(),r.value(),r.source().name(),r.sourceReference(),r.confidence(),MetadataCandidateStatus.PENDING.name(),Instant.now(),null,null))).map(this::view);}
 public Mono<List<MetadataCandidateView>> list(UUID owner,UUID resource){return owned(owner,resource).thenMany(candidates.findAllByResourceIdOrderByCreatedAtDesc(resource).take(MAX_UNPAGED_RESULTS)).map(this::view).collectList();}
 private Mono<Void> owned(UUID owner,UUID resource){return resources.findByIdAndOwnerId(resource,owner).switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问"))).then();}
 private MetadataCandidateView view(MetadataCandidateEntity c){return new MetadataCandidateView(c.id(),c.resourceId(),c.fieldKey(),c.fieldValue(),run.ikaros.metadata.MetadataSource.valueOf(c.source()),c.sourceReference(),c.confidence(),MetadataCandidateStatus.valueOf(c.status()),c.createdAt(),c.resolvedAt());}
}
