package run.ikaros.offline;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record ManifestItemRequest(@NotNull UUID attachmentId, @Min(0) long sizeBytes, String sha256, boolean required) {}
