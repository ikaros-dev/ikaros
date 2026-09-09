package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("music_lyrics")
public record MusicLyricsEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("track_id") UUID trackId, String language, String type, String content,
    @Column("timing_data") String timingData, String source, String provenance,
    Double confidence, @Column("created_at") Instant createdAt, @Version Long version) {}
