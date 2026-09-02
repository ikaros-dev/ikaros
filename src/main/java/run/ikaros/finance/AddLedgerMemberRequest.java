package run.ikaros.finance; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record AddLedgerMemberRequest(@NotNull UUID principalId,@NotNull LedgerMemberRole role) {}
