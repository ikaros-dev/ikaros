package run.ikaros.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 记录权限、删除和存储操作等不可与普通 Activity 混淆的审计事实。
 */
@Table("audit_event")
public record AuditEventEntity(
    @Id UUID id,
    @Column("actor_type") String actorType,
    @Column("actor_id") UUID actorId,
    String action,
    @Column("target_type") String targetType,
    @Column("target_id") UUID targetId,
    String details,
    @Column("occurred_at") Instant occurredAt,
    @Version Long version
) {
}
