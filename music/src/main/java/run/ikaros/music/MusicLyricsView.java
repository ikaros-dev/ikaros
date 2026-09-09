package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;

public record MusicLyricsView(UUID id, UUID trackId, String language, String type,
    String content, String timingData, String source, String provenance,
    Double confidence, Instant createdAt, Long version) {}
