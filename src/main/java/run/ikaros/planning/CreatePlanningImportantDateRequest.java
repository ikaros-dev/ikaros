package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.time.Instant;
public record CreatePlanningImportantDateRequest(@NotBlank String title,String description,@NotNull Instant occursAt,String timeZone,String kind) {}
