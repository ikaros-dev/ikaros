package run.ikaros.ingestion;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CreateImportConflictRequest(@NotNull UUID planId, @NotNull UUID candidateId,
    @NotBlank @Size(max=2000) String reason, @Min(0) @Max(100) int confidence) { }
