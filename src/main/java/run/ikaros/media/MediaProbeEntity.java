package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_probe")
public record MediaProbeEntity(@Id UUID id, @Column("release_id") UUID releaseId, String container,
    Long durationMillis, Long bitrate, Integer width, Integer height, @Column("frame_rate") String frameRate,
    @Column("video_codec") String videoCodec, @Column("audio_codec") String audioCodec,
    @Column("probe_profile_version") String probeProfileVersion, String streams, Instant probedAt,
    @Version Long version) {}
