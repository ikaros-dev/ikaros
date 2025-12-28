package run.ikaros.server.core.attachment.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentStreamVo;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentService {
    Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity);

    Mono<Attachment> save(Attachment attachment);

    Mono<Attachment> upload(AttachmentUploadCondition uploadCondition);

    Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition searchCondition);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition);

    Mono<Attachment> findById(Long attachmentId);

    Mono<AttachmentEntity> findEntityById(Long attachmentId);

    Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type, @Nullable Long parentId,
                                                  String name);

    Mono<Void> removeById(Long attachmentId);

    Mono<Void> removeByIdForcibly(Long attachmentId);

    /**
     * 只删除数据库里的表纪录，不涉及文件系统.
     *
     * @param attachmentId 附件ID
     */
    Mono<Void> removeByIdOnlyRecords(Long attachmentId);

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

    Mono<List<Attachment>> findAttachmentPathDirsById(Long id);

    Mono<Boolean> existsByParentIdAndName(@Nullable Long parentId, String name);

    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                 @Nullable Long parentId, String name);

    Mono<String> getDownloadUrl(Long aid);

    Mono<String> getReadUrl(Long aid);

    Mono<AttachmentStreamVo> getStreamById(long aid);

    Mono<Flux<DataBuffer>> getStreamByIdWithRange(long aid, long start, long end);

    Mono<Flux<DataBuffer>> getStreamByIdWithoutRange(long aid);
}
