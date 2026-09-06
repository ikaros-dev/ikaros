package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface IngestionCandidateRepository extends ReactiveCrudRepository<IngestionCandidateEntity, UUID> {
    Flux<IngestionCandidateEntity> findAllByScanRunIdOrderByCreatedAtAsc(UUID scanRunId);
}
