package run.ikaros.operations.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import run.ikaros.common.PrincipalContext;
import run.ikaros.common.PrincipalContexts;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditContext;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditService;

/**
 * 默认审计服务实现，在统一写入边界校验并脱敏结构化详情。
 */
@Service
public class DefaultAuditService implements AuditService {
    private static final Pattern ACTION_FORMAT = Pattern.compile("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9-]*)+$");
    private static final Pattern TARGET_TYPE_FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建审计服务。
     *
     * @param auditEventRepository 审计事件仓储
     */
    public DefaultAuditService(AuditEventRepository auditEventRepository) {
        this(auditEventRepository, new ObjectMapper());
    }

    @Autowired
    public DefaultAuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> record(AuditEventCommand command) {
        return Mono.defer(() -> {
            validate(command);
            return PrincipalContexts.current()
                .flatMap(principal -> save(command, mergeContext(command.context(), principal)).thenReturn(Boolean.TRUE))
                .switchIfEmpty(Mono.defer(() -> save(command, command.context()).thenReturn(Boolean.TRUE)))
                .then();
        });
    }

    private Mono<Void> save(AuditEventCommand command, AuditContext context) {
        AuditEventEntity event = new AuditEventEntity(
                null,
                command.actorType().name(),
                command.actorId(),
                command.action(),
                command.targetType(),
                command.targetId(),
                sanitizeDetails(command.detailsJson()),
                Instant.now(),
                null,
                context == null ? null : context.requestId(),
                context == null ? null : context.correlationId(),
                command.result().name(),
                command.riskLevel().name(),
                command.detailsSchemaVersion()
        );
        return auditEventRepository.save(event).then();
    }

    private AuditContext mergeContext(AuditContext requested, PrincipalContext principal) {
        if (requested == null) return new AuditContext(principal.requestId(), principal.correlationId());
        return new AuditContext(
            requested.requestId() == null ? principal.requestId() : requested.requestId(),
            requested.correlationId() == null ? principal.correlationId() : requested.correlationId()
        );
    }

    private String sanitizeDetails(String details) {
        String normalized = details == null || details.isBlank() ? "{}" : details;
        try {
            JsonNode parsed = objectMapper.readTree(normalized);
            if (!(parsed instanceof ObjectNode objectNode)) {
                throw new IllegalArgumentException("审计详情必须为 JSON 对象");
            }
            redact(objectNode);
            return objectMapper.writeValueAsString(objectNode);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("审计详情必须为合法 JSON", exception);
        }
    }

    private void redact(ObjectNode node) {
        for (Map.Entry<String, JsonNode> field : node.properties()) {
            if (isSensitiveField(field.getKey())) {
                node.put(field.getKey(), "[REDACTED]");
            } else if (field.getValue() instanceof ObjectNode nested) {
                redact(nested);
            } else if (field.getValue().isArray()) {
                field.getValue().forEach(value -> {
                    if (value instanceof ObjectNode nested) redact(nested);
                });
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("password") || normalized.contains("token") || normalized.contains("otp")
            || normalized.contains("secret") || normalized.contains("credential") || normalized.contains("authorization")
            || normalized.contains("apikey") || normalized.contains("privatekey") || normalized.contains("verificationgrant")
            || "code".equals(normalized);
    }

    private void validate(AuditEventCommand command) {
        Objects.requireNonNull(command, "审计事件不能为空");
        Objects.requireNonNull(command.actorType(), "审计主体类型不能为空");
        Objects.requireNonNull(command.result(), "审计结果不能为空");
        Objects.requireNonNull(command.riskLevel(), "审计风险等级不能为空");
        if ((command.actorType() == AuditActorType.SYSTEM || command.actorType() == AuditActorType.ANONYMOUS)
            ? command.actorId() != null : command.actorId() == null) {
            throw new IllegalArgumentException("审计主体与 actorId 不匹配");
        }
        if (command.result() == AuditResult.UNKNOWN || command.riskLevel() == AuditRiskLevel.UNKNOWN) {
            throw new IllegalArgumentException("新审计事件不能使用 UNKNOWN 结果或风险等级");
        }
        if (command.action() == null || !ACTION_FORMAT.matcher(command.action()).matches()) {
            throw new IllegalArgumentException("审计 action 必须使用小写点分命名");
        }
        if (command.targetType() == null || !TARGET_TYPE_FORMAT.matcher(command.targetType()).matches()) {
            throw new IllegalArgumentException("审计 targetType 必须使用大写下划线命名");
        }
        if (command.detailsSchemaVersion() < 1) {
            throw new IllegalArgumentException("审计详情版本必须大于零");
        }
        if (command.context() != null
            && ((command.context().requestId() != null && command.context().requestId().length() > 128)
            || (command.context().correlationId() != null && command.context().correlationId().length() > 128))) {
            throw new IllegalArgumentException("审计关联标识长度不能超过 128");
        }
    }
}
