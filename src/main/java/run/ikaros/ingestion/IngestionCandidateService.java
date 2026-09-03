package run.ikaros.ingestion;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface IngestionCandidateService {
    Mono<IngestionCandidateView> create(UUID ownerId, UUID scanRunId, CreateCandidateRequest request);
    Mono<List<IngestionCandidateView>> list(UUID ownerId, UUID scanRunId);
}
