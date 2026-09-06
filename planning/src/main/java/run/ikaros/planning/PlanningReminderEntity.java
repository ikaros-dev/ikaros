package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_reminder")
public record PlanningReminderEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("target_type") PlanningReminderTargetType targetType, @Column("target_id") UUID targetId,
    @Column("trigger_at") Instant triggerAt, @Column("time_zone") String timeZone, String channel,
    PlanningReminderStatus status, @Column("snoozed_until") Instant snoozedUntil,
    @Column("fired_at") Instant firedAt, @Column("acknowledged_at") Instant acknowledgedAt,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
