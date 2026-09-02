package run.ikaros.audit;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 统一写入高风险和管理操作的审计记录。
 */
public interface AuditService {

    /**
     * 记录一个已发生的审计事件。
     *
     * @param actorId 执行操作的主体标识，可为空以表示系统主体
     * @param action 稳定的动作名称
     * @param targetType 被操作对象类型
     * @param targetId 被操作对象标识
     * @param details 不包含 Secret 的 JSON 详情
     * @return 写入完成信号
     */
    Mono<Void> record(UUID actorId, String action, String targetType, UUID targetId, String details);
}
