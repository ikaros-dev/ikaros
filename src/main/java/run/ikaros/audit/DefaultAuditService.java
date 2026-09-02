package run.ikaros.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 默认审计服务实现，审计详情只允许保存经调用方脱敏后的结构化信息。
 */
@Service
public class DefaultAuditService implements AuditService {
    private final AuditEventRepository auditEventRepository;

    /**
     * 创建审计服务。
     *
     * @param auditEventRepository 审计事件仓储
     */
    public DefaultAuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public Mono<Void> record(UUID actorId, String action, String targetType, UUID targetId, String details) {
        AuditEventEntity event = new AuditEventEntity(
            null,
            actorId == null ? "SYSTEM" : "USER",
            actorId,
            action,
            targetType,
            targetId,
            details,
            Instant.now(),
            null
        );
        return auditEventRepository.save(event).then();
    }
}
