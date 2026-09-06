package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdatePlanningReviewRequest(@NotNull Instant periodStart, @NotNull Instant periodEnd,
    @NotBlank String note, String wins, String challenges, String nextFocus, long expectedVersion) {}
