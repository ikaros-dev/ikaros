package run.ikaros.operations.audit;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.PrincipalContext;
import run.ikaros.common.PrincipalContexts;
import run.ikaros.operations.api.AuditService;

/**
 * 默认审计服务实现，审计详情只允许保存经调用方脱敏后的结构化信息。
 */
@Service
public class DefaultAuditService implements AuditService {
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
        "(?i)(\\\"(?:password|token|otp|secret|credential|authorization|api_key|private_key|code)\\\"\\s*:\\s*)"
            + "(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|[^,}\\s]+)");
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
        return PrincipalContexts.current().flatMap(context -> save(actorId, action, targetType, targetId, details, context))
            .switchIfEmpty(save(actorId, action, targetType, targetId, details, null));
    }

    private Mono<Void> save(UUID actorId, String action, String targetType, UUID targetId, String details,
                             PrincipalContext context) {
        AuditEventEntity event = new AuditEventEntity(
                null,
                actorId == null ? "SYSTEM" : "USER",
                actorId,
                action,
                targetType,
                targetId,
                sanitizeDetails(details),
                Instant.now(),
                null,
                context == null ? null : context.requestId(),
                context == null ? null : context.correlationId()
        );
        return auditEventRepository.save(event).then();
    }

    private String sanitizeDetails(String details) {
        String normalized = details == null || details.isBlank() ? "{}" : details;
        return SENSITIVE_JSON_FIELD.matcher(normalized).replaceAll("$1\"[REDACTED]\"");
    }
}
