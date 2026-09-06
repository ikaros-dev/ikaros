package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_track")
public record MediaTrackEntity(@Id UUID id, @Column("probe_id") UUID probeId, MediaTrackKind kind,
    @Column("stable_key") String stableKey, String language, String title, String codec,
    Integer width, Integer height, Integer channels, @Column("sample_rate") Integer sampleRate,
    Integer bitrate, boolean forced, boolean isDefault, @Column("hearing_impaired") boolean hearingImpaired) {}
