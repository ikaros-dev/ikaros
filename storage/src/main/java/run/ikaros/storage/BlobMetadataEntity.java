package run.ikaros.storage;

import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Technical metadata extracted from a Blob's immutable content.
 */
@Table("blob_metadata")
public record BlobMetadataEntity(
    @Id UUID id,
    @Column("blob_id") UUID blobId,
    @Column("field_key") String fieldKey,
    @Column("field_value") Json fieldValue,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {}
