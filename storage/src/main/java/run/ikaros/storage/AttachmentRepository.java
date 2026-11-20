package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Attachment identity persistence and relationship-aware read queries. */
public interface AttachmentRepository extends ReactiveCrudRepository<AttachmentEntity, UUID> {
    @Query("select a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, a.name as name, "
        + "a.attachment_kind as attachment_kind, a.status as status, a.created_by as created_by, "
        + "a.idempotency_key as idempotency_key, a.request_fingerprint as request_fingerprint, "
        + "a.created_at as created_at, a.updated_at as updated_at, a.version as version "
        + "from attachment a left join lateral (select resource_id from resource_attachment "
        + "where attachment_id = a.id order by created_at limit 1) ra on true "
        + "left join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.created_by = :createdBy and a.idempotency_key = :idempotencyKey limit 1")
    Mono<AttachmentReference> findReferenceByCreatedByAndIdempotencyKey(UUID createdBy, String idempotencyKey);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a join resource_attachment ra on ra.attachment_id = a.id "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "join user_resource ur on ur.resource_id = ra.resource_id and ur.user_id = :ownerId "
        + "where a.status = 1 and (:resourceId is null or ra.resource_id = :resourceId) "
        + "order by a.created_at desc, a.id desc offset :offset limit :limit")
    Flux<AttachmentReference> search(UUID ownerId, UUID resourceId, long offset, int limit);

    @Query("select count(distinct a.id) from attachment a "
        + "join resource_attachment ra on ra.attachment_id = a.id "
        + "join user_resource ur on ur.resource_id = ra.resource_id and ur.user_id = :ownerId "
        + "where a.status = 1 and (:resourceId is null or ra.resource_id = :resourceId)")
    Mono<Long> countSearch(UUID ownerId, UUID resourceId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a left join lateral (select resource_id from resource_attachment "
        + "where attachment_id = a.id order by created_at limit 1) ra on true "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.status = 1 order by a.created_at desc, a.id desc offset :offset limit :limit")
    Flux<AttachmentReference> searchAllActive(long offset, int limit);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a left join lateral (select resource_id from resource_attachment "
        + "where attachment_id = a.id order by created_at limit 1) ra on true "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.status = 1 and (a.name ilike '%' || :query || '%' or cast(a.id as text) = :query "
        + "or cast(ra.resource_id as text) = :query) "
        + "order by a.created_at desc, a.id desc offset :offset limit :limit")
    Flux<AttachmentReference> searchAllActiveByQuery(String query, long offset, int limit);

    @Query("select count(*) from attachment where status = 1")
    Mono<Long> countAllActive();

    @Query("select count(*) from attachment a where a.status = 1 and (a.name ilike '%' || :query || '%' "
        + "or cast(a.id as text) = :query or exists (select 1 from resource_attachment ra "
        + "where ra.attachment_id = a.id and cast(ra.resource_id as text) = :query))")
    Mono<Long> countAllActiveByQuery(String query);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a join resource_attachment ra on ra.attachment_id = a.id "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where ra.resource_id = :resourceId and a.status = 1 order by a.created_at desc, a.id desc")
    Flux<AttachmentReference> findActiveReferencesByResourceId(UUID resourceId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a join resource_attachment ra on ra.attachment_id = a.id "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.id = :attachmentId and ra.resource_id = :resourceId and a.status = 1")
    Mono<AttachmentReference> findActiveReferenceByIdAndResourceId(UUID attachmentId, UUID resourceId);

    @Query("select a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, a.name as name, "
        + "a.attachment_kind as attachment_kind, a.status as status, a.created_by as created_by, "
        + "a.idempotency_key as idempotency_key, a.request_fingerprint as request_fingerprint, "
        + "a.created_at as created_at, a.updated_at as updated_at, a.version as version "
        + "from attachment a join resource_attachment ra on ra.attachment_id = a.id "
        + "left join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.id = :attachmentId and ra.resource_id = :resourceId")
    Mono<AttachmentReference> findReferenceByIdAndResourceId(UUID attachmentId, UUID resourceId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a left join resource_attachment ra on ra.attachment_id = a.id "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "where a.id = :attachmentId and a.status = 1 limit 1")
    Mono<AttachmentReference> findReadableReferenceById(UUID attachmentId);

    @Query("select count(*) from attachment_blob ab join attachment a on a.id = ab.attachment_id "
        + "where ab.blob_id = :blobId and a.status <> 0")
    Mono<Long> countLiveReferencesByBlobId(UUID blobId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment_blob ab join attachment a on a.id = ab.attachment_id "
        + "left join resource_attachment ra on ra.attachment_id = a.id "
        + "where ab.blob_id = :blobId and a.status <> 0")
    Flux<AttachmentReference> findLiveReferencesByBlobId(UUID blobId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment_blob ab join attachment a on a.id = ab.attachment_id "
        + "left join resource_attachment ra on ra.attachment_id = a.id "
        + "where ab.blob_id = :blobId and a.status <> 0 order by a.created_at asc, a.id asc limit 1")
    Mono<AttachmentReference> findFirstLiveReferenceByBlobId(UUID blobId);

    @Query("select " + "a.id as id, ra.resource_id as resource_id, ab.blob_id as blob_id, "
        + "a.name as name, a.attachment_kind as attachment_kind, a.status as status, "
        + "a.created_by as created_by, a.idempotency_key as idempotency_key, "
        + "a.request_fingerprint as request_fingerprint, a.created_at as created_at, "
        + "a.updated_at as updated_at, a.version as version "
        + "from attachment a join resource_attachment ra on ra.attachment_id = a.id "
        + "join user_resource ur on ur.resource_id = ra.resource_id and ur.user_id = :ownerId "
        + "join attachment_blob ab on ab.attachment_id = a.id and ab.role = 'ORIGINAL' "
        + "join blob b on b.id = ab.blob_id "
        + "where a.status = 1 and b.sha256 = :sha256 and b.size_bytes = :sizeBytes "
        + "order by a.created_at asc, a.id asc")
    Flux<AttachmentReference> findByContentIdentity(UUID ownerId, String sha256, long sizeBytes);
}
