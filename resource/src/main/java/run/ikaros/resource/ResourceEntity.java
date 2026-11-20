package run.ikaros.resource;

import run.ikaros.resource.api.*;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Resource 的持久化身份与生命周期；内容标题和外部身份均通过独立模型维护。
 */
@Table("resource")
public record ResourceEntity(
    @Id UUID id,
    @Column("resource_type") ResourceType resourceType,
    @Column("primary_title") String primaryTitle,
    String summary,
    @Column("data_classification") ResourceClassification dataClassification,
    int status,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {
}
