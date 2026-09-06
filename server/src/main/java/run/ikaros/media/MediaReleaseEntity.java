package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_release")
public record MediaReleaseEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("playable_resource_id") UUID playableResourceId, @Column("attachment_id") UUID attachmentId,
    @Column("release_group") String releaseGroup, @Column("version_label") String versionLabel,
    MediaReleaseState state, @Column("content_fingerprint") String contentFingerprint,
    Instant createdAt, Instant updatedAt, @Version Long version) {}
