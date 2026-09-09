package run.ikaros.reading;

import java.time.Instant;
import java.util.UUID;

public record ComicImportView(UUID id, UUID sourceAttachmentId, UUID workId, UUID editionId,
    ComicImportStatus status, String errorCode, String errorMessage, Instant createdAt, Instant updatedAt) {}
