package run.ikaros.operations.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;
import run.ikaros.common.NotFoundException;

/** 按操作者和时间范围查询独立审计事实。 */
@Service
public class AuditQueryService {
    private final AuditEventRepository repository;

    public AuditQueryService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public Mono<PageResponse<AuditEventEntity>> search(UUID actorId, Instant fromTime, Instant toTime,
                                                        int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (fromTime != null && toTime != null && !fromTime.isBefore(toTime))) {
            return Mono.error(new IllegalArgumentException("审计查询参数无效"));
        }
        long offset = (long) page * size;
        return Mono.zip(repository.search(actorId, fromTime, toTime, size, offset).collectList(),
                repository.countSearch(actorId, fromTime, toTime))
            .map(result -> new PageResponse<>(result.getT1(), result.getT2(), page, size));
    }

    public Mono<AuditEventEntity> get(UUID eventId) {
        return repository.findById(eventId)
            .switchIfEmpty(Mono.error(new NotFoundException("审计事件不存在")));
    }
}
