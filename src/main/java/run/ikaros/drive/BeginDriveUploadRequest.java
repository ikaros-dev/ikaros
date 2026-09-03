package run.ikaros.drive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record BeginDriveUploadRequest(@NotNull UUID uploadSessionId, @Min(1) long reservedBytes) {}
