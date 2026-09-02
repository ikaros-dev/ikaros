package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 消费者去重记录；同一 consumer 不重复处理同一 Event。 */
@Table("event_inbox")
public record InboxEntryEntity(
    @Id UUID id,
    @Column("consumer_id") String consumerId,
    @Column("event_id") UUID eventId,
    @Column("processed_at") Instant processedAt
) {
}
