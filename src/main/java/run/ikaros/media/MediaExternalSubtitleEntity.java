package run.ikaros.media;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_external_subtitle")
public record MediaExternalSubtitleEntity(@Id UUID id, @Column("release_id") UUID releaseId,
    @Column("attachment_id") UUID attachmentId, String language, String title, String format,
    String provider, @Column("offset_millis") long offsetMillis, boolean forced,
    @Column("hearing_impaired") boolean hearingImpaired, @Version Long version) {}
