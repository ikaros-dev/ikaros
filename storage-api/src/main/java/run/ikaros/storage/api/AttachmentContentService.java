package run.ikaros.storage.api;

import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

/** Reads an authorized Attachment body for server-side, streaming consumers. */
public interface AttachmentContentService {
    Flux<DataBuffer> read(UUID ownerId, UUID attachmentId);
}
