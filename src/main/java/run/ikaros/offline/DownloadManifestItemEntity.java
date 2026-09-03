package run.ikaros.offline;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("offline_download_manifest_item")
public record DownloadManifestItemEntity(@Id UUID id, @Column("manifest_id") UUID manifestId,
    @Column("attachment_id") UUID attachmentId, @Column("size_bytes") long sizeBytes, String sha256,
    boolean required) {}
