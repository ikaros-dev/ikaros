package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Internal HTTP-facing content reader; streaming details are not part of storage-api. */
interface AttachmentContentReader {
    Mono<StorageContent> readContent(UUID ownerId, UUID attachmentId, String range);
}
