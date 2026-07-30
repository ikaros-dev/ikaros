package run.ikaros.api.core.attachment;

import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.IkarosExtensionPoint;
import run.ikaros.api.store.enums.AttachmentDriverType;

public interface AttachmentDriverFetcher extends IkarosExtensionPoint {
    default AttachmentDriverType getDriverType() {
        return AttachmentDriverType.CUSTOM;
    }

    String getDriverName();

    Flux<Attachment> getChildren(UUID driverId, UUID parentAttId, String remotePath);

    Mono<String> parseReadUrl(Attachment attachment);

    Mono<String> parseDownloadUrl(Attachment attachment);

    Flux<DataBuffer> getSteam(Attachment attachment);

    Flux<DataBuffer> getSteam(Attachment attachment, long start, long end);
}
