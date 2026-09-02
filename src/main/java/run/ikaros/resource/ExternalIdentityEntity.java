package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 将 Resource 与外部平台身份解耦的稳定映射。
 */
@Table("external_identity")
public record ExternalIdentityEntity(
    @Id UUID id,
    @Column("resource_id") UUID resourceId,
    String provider,
    @Column("external_type") String externalType,
    @Column("external_id") String externalId,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {
}
