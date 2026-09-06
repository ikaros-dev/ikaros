package run.ikaros.ingestion;
import jakarta.validation.constraints.*;
public record ResolveImportConflictRequest(@NotNull Long expectedVersion, @NotBlank @Size(max=32) String resolution) { }
