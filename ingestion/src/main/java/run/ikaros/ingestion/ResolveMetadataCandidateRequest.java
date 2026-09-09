package run.ikaros.ingestion;

import jakarta.validation.constraints.NotNull;

public record ResolveMetadataCandidateRequest(@NotNull MetadataCandidateResolution resolution) { }
