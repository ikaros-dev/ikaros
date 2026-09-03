package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

/** 受控 GC 执行器：执行前再次确认引用，避免请求与执行之间的竞态删除。 */
@Service
public class BlobGarbageCollector {
    private final BlobRepository blobs;
    private final AttachmentRepository attachments;
    private final BlobPlacementRepository placements;
    private final TransactionalOperator transaction;
    private final BlobRetentionHoldRepository holds;
    private final DeliveryLeaseService leases;
    private final StorageProviderRegistry providers;
    private final StorageContentDeleter deleter;

    public BlobGarbageCollector(BlobRepository blobs, AttachmentRepository attachments,
                                BlobPlacementRepository placements, TransactionalOperator transaction, BlobRetentionHoldRepository holds,
                                DeliveryLeaseService leases, StorageProviderRegistry providers, StorageContentDeleter deleter) {
        this.blobs = blobs; this.attachments = attachments; this.placements = placements; this.transaction = transaction; this.holds = holds;
        this.leases = leases; this.providers = providers; this.deleter = deleter;
    }

    public Mono<Integer> purge(UUID blobId) {
        Mono<Integer> purge = blobs.findById(blobId)
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> attachments.countByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(blob.id())
                .filter(count -> count == 0)
                .switchIfEmpty(Mono.error(new ConflictException("Blob 仍存在有效 Attachment 引用")))
                .then(holds.existsActiveByBlobId(blob.id(), java.time.Instant.now()))
                .flatMap(active -> active ? Mono.error(new ConflictException("Blob 存在有效 Retention Hold")) : Mono.empty())
                .then(leases.protectsBlob(blob.id()))
                .flatMap(active -> active ? Mono.error(new ConflictException("Blob 正被 Delivery Lease 保护")) : Mono.empty())
                .then(placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id()).collectList())
                .flatMap(all -> deletePlacements(blob, all)));
        return transaction.transactional(purge);
    }

    private Mono<Integer> deletePlacements(BlobEntity blob, java.util.List<BlobPlacementEntity> all) {
        return reactor.core.publisher.Flux.fromIterable(all)
            .concatMap(placement -> providers.getByKey(placement.provider())
                .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
                .flatMap(provider -> deleter.supports(provider)
                    ? deleter.delete(provider, placement, blob)
                    : Mono.error(new run.ikaros.common.StorageUnavailableException("Provider 不支持物理删除"))))
            .then(placements.deleteByBlobId(blob.id()))
            .then(blobs.deleteById(blob.id()))
            .thenReturn(all.size());
    }
}
