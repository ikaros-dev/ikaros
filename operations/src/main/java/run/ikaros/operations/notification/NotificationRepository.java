package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveCrudRepository<NotificationEntity, UUID> {
    Mono<NotificationEntity> findByEventId(UUID eventId);
    Flux<NotificationEntity> findAllByRecipientIdOrderByCreatedAtDescIdDesc(UUID recipientId);

    @Query("""
        select id, event_id, recipient_id, source, event_type, title, body, priority, status,
               task_id, resource_id, created_at, read_at, archived_at, version
          from notification
         where recipient_id = :recipientId
           and (:status = '' or status = :status)
           and (:source = '' or source = :source)
           and (:priority = '' or priority = :priority)
         order by created_at desc, id desc
         limit :limit offset :offset
        """)
    Flux<NotificationEntity> search(@Param("recipientId") UUID recipientId, @Param("status") String status,
                                    @Param("source") String source, @Param("priority") String priority,
                                    @Param("limit") int limit, @Param("offset") long offset);

    @Query("""
        select count(*)
          from notification
         where recipient_id = :recipientId
           and (:status = '' or status = :status)
           and (:source = '' or source = :source)
           and (:priority = '' or priority = :priority)
        """)
    Mono<Long> countSearch(@Param("recipientId") UUID recipientId, @Param("status") String status,
                           @Param("source") String source, @Param("priority") String priority);
}
