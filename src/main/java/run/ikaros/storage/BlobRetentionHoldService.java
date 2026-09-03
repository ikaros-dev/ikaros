package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class BlobRetentionHoldService {
    private final BlobRepository blobs;
    private final BlobRetentionHoldRepository holds;

    public BlobRetentionHoldService(BlobRepository blobs, BlobRetentionHoldRepository holds) {
        this.blobs = blobs; this.holds = holds;
    }

    public Mono<BlobRetentionHoldView> create(UUID actorId, UUID blobId, BlobRetentionHoldRequest request) {
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now()))
            return Mono.error(new IllegalArgumentException("Retention Hold 过期时间必须在未来"));
        return blobs.findById(blobId).switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .then(holds.findByBlobIdAndHolderTypeAndHolderIdAndReasonCode(blobId, request.holderType().trim(),
                request.holderId().trim(), request.reasonCode().trim())
                .filter(old -> old.releasedAt() == null)
                .switchIfEmpty(Mono.defer(() -> holds.save(new BlobRetentionHoldEntity(null, blobId,
                    request.holderType().trim(), request.holderId().trim(), request.reasonCode().trim(), request.expiresAt(),
                    actorId, Instant.now(), null, null))))
                .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("Retention Hold 已存在")))
            .map(this::view);
    }

    public Flux<BlobRetentionHoldView> list(UUID actorId, UUID blobId) {
        return blobs.findById(blobId).switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .thenMany(holds.findAllByBlobIdAndReleasedAtIsNullOrderByCreatedAtAsc(blobId).take(100).filter(h -> h.createdBy().equals(actorId)).map(this::view));
    }

    public Mono<Void> release(UUID actorId, UUID holdId) {
        return holds.findByIdAndCreatedBy(holdId, actorId)
            .switchIfEmpty(Mono.error(new NotFoundException("Retention Hold 不存在或无权访问")))
            .flatMap(old -> old.releasedAt() != null ? Mono.just(old) : holds.save(new BlobRetentionHoldEntity(old.id(), old.blobId(),
                old.holderType(), old.holderId(), old.reasonCode(), old.expiresAt(), old.createdBy(), old.createdAt(), Instant.now(), old.version())))
            .then();
    }

    private BlobRetentionHoldView view(BlobRetentionHoldEntity h) {
        return new BlobRetentionHoldView(h.id(), h.blobId(), h.holderType(), h.holderId(), h.reasonCode(), h.expiresAt(),
            h.createdBy(), h.createdAt(), h.releasedAt());
    }
}
