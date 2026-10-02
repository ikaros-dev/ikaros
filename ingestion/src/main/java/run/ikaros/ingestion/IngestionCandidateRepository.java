package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IngestionCandidateRepository extends ReactiveCrudRepository<IngestionCandidateEntity, UUID> {
    Flux<IngestionCandidateEntity> findAllByScanRunIdOrderByCreatedAtAsc(UUID scanRunId);
    Mono<IngestionCandidateEntity> findByScanRunIdAndFingerprint(UUID scanRunId, String fingerprint);
}
