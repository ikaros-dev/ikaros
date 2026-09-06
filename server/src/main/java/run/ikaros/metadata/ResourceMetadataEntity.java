package run.ikaros.metadata;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 单个 Resource 字段的当前值、来源和人工覆盖状态。 */
@Table("resource_metadata")
public record ResourceMetadataEntity(@Id UUID id, @Column("resource_id") UUID resourceId,
                                     @Column("field_key") String fieldKey, @Column("field_value") String value,
                                     MetadataSource source, @Column("source_reference") String sourceReference,
                                     @Column("manually_locked") boolean manuallyLocked,
                                     @Column("updated_at") Instant updatedAt, @Version Long version) { }
