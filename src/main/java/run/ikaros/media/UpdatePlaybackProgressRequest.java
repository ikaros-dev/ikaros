package run.ikaros.media;

import jakarta.validation.constraints.Min;

public record UpdatePlaybackProgressRequest(@Min(0) long positionSeconds, Long totalSeconds, boolean completed) {}
