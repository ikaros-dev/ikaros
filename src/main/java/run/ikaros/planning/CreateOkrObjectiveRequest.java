package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CreateOkrObjectiveRequest(@NotNull UUID cycleId,@NotBlank String title,String description,UUID goalId) {}
