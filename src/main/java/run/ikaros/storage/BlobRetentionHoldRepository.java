package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BlobRetentionHoldRepository extends ReactiveCrudRepository<BlobRetentionHoldEntity, UUID> {
    Mono<BlobRetentionHoldEntity> findByBlobIdAndHolderTypeAndHolderIdAndReasonCode(UUID blobId, String holderType,
        String holderId, String reasonCode);
    Flux<BlobRetentionHoldEntity> findAllByBlobIdAndReleasedAtIsNullOrderByCreatedAtAsc(UUID blobId);
    Mono<BlobRetentionHoldEntity> findByIdAndCreatedBy(UUID id, UUID createdBy);
    @Query("select exists (select 1 from blob_retention_hold where blob_id = :blobId and released_at is null and (expires_at is null or expires_at > :now))")
    Mono<Boolean> existsActiveByBlobId(UUID blobId, Instant now);
}
