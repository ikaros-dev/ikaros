package run.ikaros.finance; import jakarta.validation.constraints.NotBlank; public record CreateLedgerRequest(@NotBlank String name,@NotBlank String baseCurrency) {}
