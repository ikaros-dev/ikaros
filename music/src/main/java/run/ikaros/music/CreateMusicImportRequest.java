package run.ikaros.music;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMusicImportRequest(@NotNull UUID attachmentId, String title,
    Long durationMillis, String codec, String container, Integer sampleRate,
    Integer bitDepth, Integer channels, Integer bitrate, boolean lossless) {}
