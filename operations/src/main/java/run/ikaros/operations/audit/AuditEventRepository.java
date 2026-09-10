package run.ikaros.operations.audit;

import java.util.UUID;
import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * 审计事件的数据库访问边界。
 */
public interface AuditEventRepository extends ReactiveCrudRepository<AuditEventEntity, UUID> {
    @Query("""
        select id, actor_type, actor_id, action, target_type, target_id, details, occurred_at, version,
               request_id, correlation_id
        from audit_event
        where (:actorId is null or actor_id = :actorId)
          and (:requestId = '' or request_id = :requestId)
          and (:fromTime is null or occurred_at >= :fromTime)
          and (:toTime is null or occurred_at < :toTime)
        order by occurred_at desc, id desc
        limit :limit offset :offset
        """)
    Flux<AuditEventEntity> search(@Param("actorId") UUID actorId, @Param("requestId") String requestId,
                                  @Param("fromTime") Instant fromTime,
                                  @Param("toTime") Instant toTime, @Param("limit") int limit,
                                  @Param("offset") long offset);

    @Query("""
        select count(*)
        from audit_event
        where (:actorId is null or actor_id = :actorId)
          and (:requestId = '' or request_id = :requestId)
          and (:fromTime is null or occurred_at >= :fromTime)
          and (:toTime is null or occurred_at < :toTime)
        """)
    Mono<Long> countSearch(@Param("actorId") UUID actorId, @Param("requestId") String requestId,
                           @Param("fromTime") Instant fromTime,
                           @Param("toTime") Instant toTime);
}
