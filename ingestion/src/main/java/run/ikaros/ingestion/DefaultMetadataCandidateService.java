package run.ikaros.ingestion;
import java.time.Instant; import java.util.List; import java.util.UUID; import org.springframework.stereotype.Service; import reactor.core.publisher.Mono;
import run.ikaros.resource.api.AutomaticMetadataRequest; import run.ikaros.resource.api.MetadataSource; import run.ikaros.resource.api.ResourceMetadataService; import run.ikaros.resource.api.ResourceOwnershipQuery;
@Service public class DefaultMetadataCandidateService implements MetadataCandidateService {
 private static final int MAX_UNPAGED_RESULTS = 100;
 private final ResourceOwnershipQuery resources; private final MetadataCandidateRepository candidates; private final ResourceMetadataService metadata;
 public DefaultMetadataCandidateService(ResourceOwnershipQuery resources,MetadataCandidateRepository candidates,ResourceMetadataService metadata){this.resources=resources;this.candidates=candidates;this.metadata=metadata;}
 public Mono<MetadataCandidateView> submit(UUID owner,UUID resource,SubmitMetadataCandidateRequest r){return owned(owner,resource).then(candidates.save(new MetadataCandidateEntity(null,resource,r.fieldKey(),r.value(),r.source().name(),r.sourceReference(),r.confidence(),MetadataCandidateStatus.PENDING.name(),Instant.now(),null,null))).map(this::view);}
 public Mono<List<MetadataCandidateView>> list(UUID owner,UUID resource){return owned(owner,resource).thenMany(candidates.findAllByResourceIdOrderByCreatedAtDesc(resource).take(MAX_UNPAGED_RESULTS)).map(this::view).collectList();}
 public Mono<MetadataCandidateView> resolve(UUID owner, UUID candidateId, ResolveMetadataCandidateRequest request) {
  return candidates.findById(candidateId).switchIfEmpty(Mono.error(new IllegalArgumentException("元数据候选不存在")))
   .flatMap(candidate -> owned(owner, candidate.resourceId()).then(resolveCandidate(owner, candidate, request.resolution())));
 }
 private Mono<MetadataCandidateView> resolveCandidate(UUID owner, MetadataCandidateEntity candidate, MetadataCandidateResolution resolution) {
  MetadataCandidateStatus nextStatus = resolution == MetadataCandidateResolution.APPLY ? MetadataCandidateStatus.APPLIED : MetadataCandidateStatus.REJECTED;
  MetadataCandidateStatus currentStatus = MetadataCandidateStatus.valueOf(candidate.status());
  if (currentStatus != MetadataCandidateStatus.PENDING) {
   return currentStatus == nextStatus ? Mono.just(view(candidate)) : Mono.error(new IllegalStateException("元数据候选已经处理"));
  }
  if (resolution == MetadataCandidateResolution.REJECT) return saveResolution(candidate, nextStatus);
  return metadata.applyAutomatic(owner, candidate.resourceId(), candidate.fieldKey(), new AutomaticMetadataRequest(candidate.fieldValue(), MetadataSource.valueOf(candidate.source()), candidate.sourceReference()))
   .flatMap(applied -> applied.applied() ? saveResolution(candidate, nextStatus) : Mono.error(new IllegalStateException("字段已被人工锁定，候选未应用")));
 }
 private Mono<MetadataCandidateView> saveResolution(MetadataCandidateEntity candidate, MetadataCandidateStatus status) {
  return candidates.save(new MetadataCandidateEntity(candidate.id(), candidate.resourceId(), candidate.fieldKey(), candidate.fieldValue(), candidate.source(), candidate.sourceReference(), candidate.confidence(), status.name(), candidate.createdAt(), Instant.now(), candidate.version())).map(this::view);
 }
 private Mono<Void> owned(UUID owner,UUID resource){return resources.requireOwned(owner,resource);}
 private MetadataCandidateView view(MetadataCandidateEntity c){return new MetadataCandidateView(c.id(),c.resourceId(),c.fieldKey(),c.fieldValue(),MetadataSource.valueOf(c.source()),c.sourceReference(),c.confidence(),MetadataCandidateStatus.valueOf(c.status()),c.createdAt(),c.resolvedAt());}
}
