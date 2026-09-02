package run.ikaros.planning;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePlanningReminderRequest(@NotNull PlanningReminderTargetType targetType,
    @NotNull UUID targetId, @NotNull Instant triggerAt, String timeZone, String channel) {}
