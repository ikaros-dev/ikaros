package run.ikaros.api.core.attachment;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.wrap.PagingWrap;

public interface AttachmentDriverOperate extends AllowPluginOperate {

    Mono<AttachmentDriver> save(AttachmentDriver driver);

    Mono<AttachmentDriver> findById(UUID id);

    Mono<AttachmentDriver> findByTypeAndName(String type, String name);

    Mono<PagingWrap<Attachment>> listAttachmentsByCondition(
        AttachmentSearchCondition attachmentSearchCondition);

    Mono<Void> refresh(UUID attachmentId);

    Mono<PagingWrap<AttachmentDriver>> listDriversByCondition(
        Integer page, Integer pageSize
    );
}
