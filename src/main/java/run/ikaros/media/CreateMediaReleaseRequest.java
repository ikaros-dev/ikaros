package run.ikaros.media;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMediaReleaseRequest(@NotNull UUID attachmentId, String releaseGroup, String versionLabel,
    String contentFingerprint) {}
