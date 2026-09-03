package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePlanningTimeBlockRequest(@NotBlank String title, UUID taskId, @NotNull Instant startAt,
    @NotNull Instant endAt, PlanningTimeBlockKind kind, String timeZone) {}
