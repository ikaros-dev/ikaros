package run.ikaros.planning;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreatePlanningHabitCheckInRequest(@NotNull Double value, Instant occurredAt, String note) {}
