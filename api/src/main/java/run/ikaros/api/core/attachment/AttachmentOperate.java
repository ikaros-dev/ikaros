package run.ikaros.api.core.attachment;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;

public interface AttachmentOperate extends AllowPluginOperate {

    Mono<Attachment> save(Attachment attachment);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition);

    Mono<Attachment> upload(AttachmentUploadCondition uploadCondition);

    Mono<Attachment> findById(Long attachmentId);

    Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type, @Nullable Long parentId,
                                                  String name);

    Mono<Attachment> createDirectory(@Nullable Long parentId, @NotBlank String name);

    Mono<Boolean> existsByParentIdAndName(@Nullable Long parentId, String name);

    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                 @Nullable Long parentId, String name);
}
