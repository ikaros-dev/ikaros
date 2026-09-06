package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record MediaSubjectView(UUID id, UUID resourceId, MediaSubjectKind kind, Instant createdAt) {}
