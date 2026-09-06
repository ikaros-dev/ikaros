package run.ikaros.media;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartPlaybackRequest(@NotNull UUID releaseId, long startPositionSeconds) {}
