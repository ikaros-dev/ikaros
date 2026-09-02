package run.ikaros.audit;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * 审计事件的数据库访问边界。
 */
public interface AuditEventRepository extends ReactiveCrudRepository<AuditEventEntity, UUID> {
}
