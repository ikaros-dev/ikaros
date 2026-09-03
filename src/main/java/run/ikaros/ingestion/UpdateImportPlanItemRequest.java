package run.ikaros.ingestion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
public record UpdateImportPlanItemRequest(@NotNull Long expectedVersion, @NotNull ImportAction action,
    UUID targetId, @Size(max=2000) String reason) { }
