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

    /**
     * 计算附件内容摘要.
     *
     * @param attachment 附件元数据
     * @return 已填充内容摘要的附件
     */
    default Mono<Attachment> calculateSha1(Attachment attachment) {
        return Mono.just(attachment);
    }

    Mono<String> parseReadUrl(Attachment attachment);

    Mono<String> parseDownloadUrl(Attachment attachment);

    Flux<DataBuffer> getSteam(Attachment attachment);

    Flux<DataBuffer> getSteam(Attachment attachment, long start, long end);
}
