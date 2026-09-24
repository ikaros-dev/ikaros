package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("event_delivery")
public record OutboxDeliveryEntity(
    @Id UUID id,
    @Column("consumer_id") String consumerId,
    @Column("event_id") UUID eventId,
    String status,
    @Column("attempt_count") int attemptCount,
    @Column("next_attempt_at") Instant nextAttemptAt,
    @Column("last_attempt_at") Instant lastAttemptAt,
    @Column("last_error_classification") String lastErrorClassification,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt
) {
}
