package run.ikaros.offline;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateCacheEntryRequest(@NotNull UUID deviceId, @NotNull UUID resourceId,
    UUID attachmentId, @Min(0) long sizeBytes, String contentFingerprint) {}
