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

    public BlobGarbageCollector(BlobRepository blobs, AttachmentRepository attachments,
                                BlobPlacementRepository placements, TransactionalOperator transaction) {
        this.blobs = blobs; this.attachments = attachments; this.placements = placements; this.transaction = transaction;
    }

    public Mono<Void> purge(UUID blobId) {
        return transaction.transactional(blobs.findById(blobId)
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> attachments.countByBlobIdAndDeletedAtIsNull(blob.id())
                .filter(count -> count == 0)
                .switchIfEmpty(Mono.error(new ConflictException("Blob 仍存在有效 Attachment 引用")))
                .then(placements.deleteByBlobId(blob.id()))
                .then(blobs.deleteById(blob.id())))
            .then());
    }
}
