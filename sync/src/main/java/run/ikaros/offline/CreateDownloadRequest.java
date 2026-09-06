package run.ikaros.offline;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateDownloadRequest(@NotNull UUID deviceId, @NotNull UUID resourceId,
    UUID attachmentId, OfflineCopyKind kind) {}
