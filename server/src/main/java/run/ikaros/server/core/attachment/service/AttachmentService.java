package run.ikaros.server.core.attachment.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentService {
    Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity);

    Mono<Attachment> save(Attachment attachment);

    Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition searchCondition);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition);

    Mono<Void> removeById(Long attachmentId);

    Mono<Attachment> upload(AttachmentUploadCondition uploadCondition);

    Mono<Attachment> findById(Long attachmentId);

    Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type, @Nullable Long parentId,
                                                  String name);

    Mono<Void> removeByTypeAndParentIdAndName(
        AttachmentType type, @Nullable Long parentId, String name);


    Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                       @Nonnull Long uploadLength,
                                                       @Nonnull Long uploadOffset,
                                                       @NotBlank String uploadName,
                                                       byte[] bytes,
                                                       @Nullable Long parentId);

    Mono<Void> revertFragmentUploadFile(@NotBlank String unique);

    Mono<Attachment> createDirectory(@Nullable Long parentId, @NotBlank String name);

}
