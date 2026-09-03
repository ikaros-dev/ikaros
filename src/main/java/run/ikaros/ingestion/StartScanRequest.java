package run.ikaros.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartScanRequest(@NotBlank @Size(max = 64) String trigger) { }
