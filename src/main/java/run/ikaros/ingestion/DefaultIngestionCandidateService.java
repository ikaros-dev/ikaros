package run.ikaros.ingestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class DefaultIngestionCandidateService implements IngestionCandidateService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final ScanRunRepository scans;
    private final IngestionCandidateRepository candidates;
    public DefaultIngestionCandidateService(ScanRunRepository scans, IngestionCandidateRepository candidates) {
        this.scans = scans; this.candidates = candidates;
    }
    @Override public Mono<IngestionCandidateView> create(UUID ownerId, UUID scanRunId, CreateCandidateRequest request) {
        return scans.findByIdAndOwnerId(scanRunId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .flatMap(scan -> candidates.save(new IngestionCandidateEntity(null, scanRunId, scan.sourceId(),
                request.suggestedResourceType(), request.titleHint(), request.externalIdHint(), request.confidence(),
                request.fingerprint(), CandidateStatus.NEW.name(), Instant.now(), null)))
            .map(this::view);
    }
    @Override public Mono<List<IngestionCandidateView>> list(UUID ownerId, UUID scanRunId) {
        return scans.findByIdAndOwnerId(scanRunId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .thenMany(candidates.findAllByScanRunIdOrderByCreatedAtAsc(scanRunId).take(MAX_UNPAGED_RESULTS))
            .map(this::view).collectList();
    }
    private IngestionCandidateView view(IngestionCandidateEntity c) {
        return new IngestionCandidateView(c.id(), c.scanRunId(), c.sourceId(), c.suggestedResourceType(), c.titleHint(),
            c.externalIdHint(), c.confidence(), c.fingerprint(), CandidateStatus.valueOf(c.status()), c.createdAt());
    }
}
