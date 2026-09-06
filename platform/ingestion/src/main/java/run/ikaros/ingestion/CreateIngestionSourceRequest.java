package run.ikaros.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateIngestionSourceRequest(
    @NotNull IngestionSourceType type,
    @NotBlank @Size(max = 256) String displayName,
    @NotBlank @Size(max = 1024) String rootReference,
    @Size(max = 512) String credentialReference,
    Map<String, Object> scanPolicy
) { }
