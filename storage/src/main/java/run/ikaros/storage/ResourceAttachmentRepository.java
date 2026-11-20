package run.ikaros.storage;

import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public final class ResourceAttachmentRepository {
    private final DatabaseClient databaseClient;

    public ResourceAttachmentRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> save(UUID resourceId, UUID attachmentId, java.time.Instant createdAt) {
        return databaseClient.sql("insert into resource_attachment (resource_id, attachment_id, created_at, version) "
                + "values (:resourceId, :attachmentId, :createdAt, 0) on conflict (resource_id, attachment_id) do nothing")
            .bind("resourceId", resourceId)
            .bind("attachmentId", attachmentId)
            .bind("createdAt", createdAt)
            .fetch()
            .rowsUpdated()
            .then();
    }

    public Mono<Void> deleteByResourceIdAndAttachmentId(UUID resourceId, UUID attachmentId) {
        return databaseClient.sql("delete from resource_attachment where resource_id = :resourceId "
                + "and attachment_id = :attachmentId")
            .bind("resourceId", resourceId)
            .bind("attachmentId", attachmentId)
            .fetch()
            .rowsUpdated()
            .then();
    }
}
