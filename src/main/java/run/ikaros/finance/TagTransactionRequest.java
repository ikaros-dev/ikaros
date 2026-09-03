package run.ikaros.finance; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record TagTransactionRequest(@NotNull UUID tagId) {}
