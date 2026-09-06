package run.ikaros.planning;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdatePlanningTimeBlockRequest(@NotNull Instant startAt, @NotNull Instant endAt,
    PlanningTimeBlockKind kind, String timeZone, long expectedVersion) {}
