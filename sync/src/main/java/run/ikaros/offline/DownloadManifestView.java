package run.ikaros.offline;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record DownloadManifestView(UUID id, UUID intentId, long version, List<ManifestItemView> items, Instant generatedAt) {}
