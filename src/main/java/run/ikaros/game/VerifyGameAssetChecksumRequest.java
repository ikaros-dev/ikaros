package run.ikaros.game; import jakarta.validation.constraints.NotBlank; public record VerifyGameAssetChecksumRequest(@NotBlank String algorithm,@NotBlank String value,boolean matched) {}
