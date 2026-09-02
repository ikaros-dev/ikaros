package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningReviewView(UUID id, UUID ownerId, PlanningReviewPeriod period, Instant periodStart,
    Instant periodEnd, String note, String wins, String challenges, String nextFocus, Instant createdAt,
    Instant updatedAt, long version) {}
