package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_review")
public record PlanningReviewEntity(@Id UUID id, @Column("owner_id") UUID ownerId, PlanningReviewPeriod period,
    @Column("period_start") Instant periodStart, @Column("period_end") Instant periodEnd, String note,
    String wins, String challenges, @Column("next_focus") String nextFocus, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
