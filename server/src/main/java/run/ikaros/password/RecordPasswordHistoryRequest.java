package run.ikaros.password; import jakarta.validation.constraints.NotBlank; public record RecordPasswordHistoryRequest(long revision,@NotBlank String encryptedPayload) {}
