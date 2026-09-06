package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Resource 的本地化标题，允许同一资源同时具有多个语言标题。
 */
@Table("resource_title")
public record ResourceTitleEntity(
    @Id UUID id,
    @Column("resource_id") UUID resourceId,
    String locale,
    String title,
    @Column("is_primary") boolean primary,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version,
    @Column("title_kind") ResourceTitleKind titleKind
) {
    public ResourceTitleEntity(UUID id, UUID resourceId, String locale, String title, boolean primary,
                               Instant createdAt, Instant updatedAt, Long version) {
        this(id, resourceId, locale, title, primary, createdAt, updatedAt, version, ResourceTitleKind.TITLE);
    }
}
