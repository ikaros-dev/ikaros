package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;

public record IngestionCandidateView(UUID id, UUID scanRunId, UUID sourceId, String suggestedResourceType,
    String titleHint, String externalIdHint, int confidence, String fingerprint, CandidateStatus status,
    Instant createdAt) { }
