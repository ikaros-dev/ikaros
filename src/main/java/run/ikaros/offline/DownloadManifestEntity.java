package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("offline_download_manifest")
public record DownloadManifestEntity(@Id UUID id, @Column("intent_id") UUID intentId, long manifestVersion,
    @Column("generated_at") Instant generatedAt) {}
