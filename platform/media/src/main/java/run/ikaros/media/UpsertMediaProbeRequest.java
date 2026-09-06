package run.ikaros.media;

import jakarta.validation.constraints.NotBlank;

public record UpsertMediaProbeRequest(@NotBlank String probeProfileVersion, String container, Long durationMillis,
    Long bitrate, Integer width, Integer height, String frameRate, String videoCodec, String audioCodec, String streams) {}
