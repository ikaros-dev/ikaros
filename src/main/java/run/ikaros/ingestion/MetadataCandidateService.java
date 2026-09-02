package run.ikaros.ingestion;
import java.util.List; import java.util.UUID; import reactor.core.publisher.Mono;
public interface MetadataCandidateService { Mono<MetadataCandidateView> submit(UUID owner,UUID resource,SubmitMetadataCandidateRequest request); Mono<List<MetadataCandidateView>> list(UUID owner,UUID resource); }
