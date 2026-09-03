package run.ikaros.notes; import jakarta.validation.constraints.NotBlank; public record CreateVaultRequest(@NotBlank String name,@NotBlank String cryptoContext,String encryptedRecoveryKey) {}
