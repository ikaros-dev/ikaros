package run.ikaros.ingestion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DetectMetadataUpdateRequest(
    @JsonProperty("resource_id") @NotNull UUID resourceId,
    @JsonProperty("field_key") @NotBlank @Size(max = 128) String fieldKey,
    @NotBlank String value,
    @JsonProperty("source_reference") @Size(max = 512) String sourceReference,
    @Min(0) @Max(100) int confidence
) { }
