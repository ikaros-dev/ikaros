package run.ikaros.media;

import jakarta.validation.constraints.NotNull;

public record UpdateMediaReleaseStateRequest(@NotNull MediaReleaseState state) {}
