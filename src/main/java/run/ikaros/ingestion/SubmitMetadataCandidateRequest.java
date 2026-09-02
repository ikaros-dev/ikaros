package run.ikaros.ingestion;
import jakarta.validation.constraints.*; import run.ikaros.metadata.MetadataSource;
public record SubmitMetadataCandidateRequest(@NotBlank @Size(max=128) String fieldKey,@NotBlank String value,
 @NotNull MetadataSource source,@Size(max=512) String sourceReference,@Min(0) @Max(100) int confidence) { }
