package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AttachmentBlobRepository extends ReactiveCrudRepository<AttachmentBlobEntity, UUID> {
    Flux<AttachmentBlobEntity> findAllByAttachmentId(UUID attachmentId);

    Flux<AttachmentBlobEntity> findAllByBlobId(UUID blobId);

    Mono<AttachmentBlobEntity> findByAttachmentIdAndRole(UUID attachmentId, String role);

    Mono<AttachmentBlobEntity> findByAttachmentIdAndBlobId(UUID attachmentId, UUID blobId);

    Mono<Void> deleteByAttachmentIdAndBlobId(UUID attachmentId, UUID blobId);
}
