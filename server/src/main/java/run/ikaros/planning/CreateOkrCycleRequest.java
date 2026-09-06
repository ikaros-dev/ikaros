package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.time.Instant;
public record CreateOkrCycleRequest(@NotBlank String name,@NotNull Instant startAt,@NotNull Instant endAt) {}
