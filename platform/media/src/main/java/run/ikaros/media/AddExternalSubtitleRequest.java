package run.ikaros.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddExternalSubtitleRequest(@NotNull UUID attachmentId, @NotBlank String language, String title,
    @NotBlank String format, String provider, long offsetMillis, boolean forced, boolean hearingImpaired) {}
