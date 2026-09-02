package run.ikaros.planning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreatePlanningTimeEntryRequest(@Min(1) int durationMinutes, Instant startedAt,
    Instant endedAt, PlanningTimeEntrySource source, String note) {}
