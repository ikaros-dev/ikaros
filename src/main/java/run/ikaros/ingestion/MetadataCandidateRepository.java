package run.ikaros.ingestion;
import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Flux;
public interface MetadataCandidateRepository extends ReactiveCrudRepository<MetadataCandidateEntity,UUID>{
 Flux<MetadataCandidateEntity> findAllByResourceIdOrderByCreatedAtDesc(UUID resourceId);
}
