package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 事务内写入、发布后只更新投递元数据的 Outbox 事件。 */
@Table("event_outbox")
public record OutboxEventEntity(
    @Id UUID id,
    @Column("event_type") String eventType,
    @Column("schema_version") int schemaVersion,
    @Column("aggregate_type") String aggregateType,
    @Column("aggregate_id") UUID aggregateId,
    @Column("payload_json") String payloadJson,
    @Column("occurred_at") Instant occurredAt,
    @Column("attempt_count") int attemptCount,
    @Column("last_attempt_at") Instant lastAttemptAt,
    @Column("dispatched_at") Instant dispatchedAt
) {
}
