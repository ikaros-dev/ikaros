package run.ikaros.offline;
import java.util.UUID;
public record ManifestItemView(UUID id, UUID attachmentId, long sizeBytes, String sha256, boolean required) {}
