package run.ikaros.password; import jakarta.validation.constraints.NotBlank; public record RegisterDeviceRequest(@NotBlank String name,@NotBlank String deviceType) {}
