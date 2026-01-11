package run.ikaros.api.core.attachment;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;

public interface AttachmentOperate extends AllowPluginOperate {

    Mono<Attachment> save(Attachment attachment);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition);

    Mono<Attachment> upload(AttachmentUploadCondition uploadCondition);

    Mono<Attachment> findById(UUID attachmentId);

    Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type, UUID parentId,
                                                  String name);

    Mono<Attachment> createDirectory(UUID parentId, @NotBlank String name);

    Mono<Boolean> existsByParentIdAndName(UUID parentId, String name);

    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                 UUID parentId, String name);
}
