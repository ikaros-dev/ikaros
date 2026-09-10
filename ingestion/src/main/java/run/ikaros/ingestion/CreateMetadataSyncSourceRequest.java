package run.ikaros.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMetadataSyncSourceRequest(
    @NotBlank @Size(max = 128) String providerKey,
    @NotBlank @Size(max = 256) String displayName,
    @Size(max = 512) String credentialReference,
    @NotBlank @Size(max = 32) String refreshSchedule
) { }
