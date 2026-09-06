package run.ikaros.ingestion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCandidateRequest(@NotBlank @Size(max = 64) String suggestedResourceType,
    @Size(max = 512) String titleHint, @Size(max = 512) String externalIdHint,
    @Min(0) @Max(100) int confidence, @NotBlank @Size(max = 512) String fingerprint) { }
