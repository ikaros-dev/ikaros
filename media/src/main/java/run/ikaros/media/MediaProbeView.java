package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record MediaProbeView(UUID id, UUID releaseId, String container, Long durationMillis, Long bitrate,
    Integer width, Integer height, String frameRate, String videoCodec, String audioCodec,
    String probeProfileVersion, String streams, Instant probedAt) {}
