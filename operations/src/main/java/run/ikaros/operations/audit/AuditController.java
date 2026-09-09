package run.ikaros.operations.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/** 操作审计查询入口；访问门禁由统一授权过滤器执行。 */
@RestController
@RequestMapping("/api/audit-events")
public class AuditController {
    private final AuditQueryService service;

    public AuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    public Mono<PageResponse<AuditEventEntity>> search(
        @RequestHeader("X-Ikaros-Actor-Id") UUID requesterId,
        @RequestParam(name = "actor_id", required = false) UUID actorId,
        @RequestParam(name = "request_id", required = false) String requestId,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(actorId, requestId, from, to, page, size);
    }

    @GetMapping("/{eventId}")
    public Mono<AuditEventEntity> get(@PathVariable UUID eventId,
                                      @RequestHeader("X-Ikaros-Actor-Id") UUID requesterId) {
        return service.get(eventId);
    }
}
