package run.ikaros.notes; import jakarta.validation.constraints.NotBlank; public record UpdateRecoveryKeyRequest(@NotBlank String encryptedRecoveryKey) {}
