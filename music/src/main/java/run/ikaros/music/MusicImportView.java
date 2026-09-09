package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;

public record MusicImportView(UUID id, UUID attachmentId, UUID trackId, String title,
    Long durationMillis, String status, String errorCode, String errorMessage,
    Instant createdAt, Instant updatedAt) {}
