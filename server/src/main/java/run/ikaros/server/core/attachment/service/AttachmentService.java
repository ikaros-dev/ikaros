package run.ikaros.server.core.attachment.service;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AccessUrlCondition;
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

    /**
     * 验证并上传普通附件，验证失败时不创建附件记录.
     *
     * @param uploadCondition 上传名称、父目录和数据流
     * @return 已保存的附件
     */
    Mono<Attachment> upload(AttachmentUploadCondition uploadCondition);

    Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition searchCondition);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition);

    Mono<Attachment> findById(UUID attachmentId);

    Mono<AttachmentEntity> findEntityById(UUID attachmentId);

    Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type, @Nullable UUID parentId,
                                                  String name);

    Mono<Void> removeById(UUID attachmentId);

    Mono<Void> removeByIdForcibly(UUID attachmentId);

    /**
     * 只删除数据库里的表纪录，不涉及文件系统.
     *
     * @param attachmentId 附件ID
     */
    Mono<Void> removeByIdOnlyRecords(UUID attachmentId);

    Mono<Void> removeByTypeAndParentIdAndName(
        AttachmentType type, @Nullable UUID parentId, String name);

    /**
     * 接收分片数据流，并在会话完成后验证、合并和保存附件.
     *
     * @param unique 分片上传会话标识
     * @param uploadLength 完整文件长度
     * @param uploadOffset 当前分片起始偏移量
     * @param uploadName 上传文件名
     * @param content 当前分片数据流
     * @param parentId 附件父目录 ID
     * @return 分片处理完成信号
     */
    Mono<Void> receiveAndHandleFragmentUploadChunkFile(@NotBlank String unique,
                                                       @NonNull Long uploadLength,
                                                       @NonNull Long uploadOffset,
                                                       @NotBlank String uploadName,
                                                       Flux<DataBuffer> content,
                                                       @Nullable UUID parentId);

    /**
     * 清理指定分片上传会话的临时资源.
     *
     * @param unique 分片上传会话标识
     * @return 清理完成信号
     */
    Mono<Void> revertFragmentUploadFile(@NotBlank String unique);

    Mono<Attachment> createDirectory(@Nullable UUID parentId, @NotBlank String name);

    Mono<List<Attachment>> findAttachmentPathDirsById(UUID id);

    Mono<Boolean> existsByParentIdAndName(@Nullable UUID parentId, String name);

    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                 @Nullable UUID parentId, String name);

    Mono<String> getDownloadUrl(UUID aid);

    Mono<String> getReadUrl(UUID aid);

    /**
     * 验证附件真实格式并获取完整内容流.
     *
     * @param aid 附件 ID
     * @return 包含真实 MIME、内容长度和完整内容的附件流
     */
    Mono<AttachmentStreamVo> getStreamById(UUID aid);

    /**
     * 先从文件头验证附件真实格式，再获取指定范围内容流.
     *
     * @param aid 附件 ID
     * @param start 范围起始位置，包含该位置
     * @param end 范围结束位置，包含该位置
     * @return 包含真实 MIME、范围长度和范围内容的附件流
     */
    Mono<AttachmentStreamVo> getStreamByIdWithRange(UUID aid, long start, long end);

    /**
     * 验证附件真实格式并获取完整内容数据流.
     *
     * @param aid 附件 ID
     * @return 完整内容数据流
     */
    Mono<Flux<DataBuffer>> getStreamByIdWithoutRange(UUID aid);

    /**
     * 根据附件ID和条件参数获取访问地址（由插件实现的AttachmentAccessUrlProvider提供）.
     * 若无匹配的provider，回退到默认的getReadUrl().
     *
     * @param attachmentId 附件ID
     * @param conditions   条件参数，如 {"quality":"4k","vipToken":"xxx"}
     */
    Mono<String> getUrlWithConditions(UUID attachmentId, Map<String, Object> conditions);

    /**
     * 获取指定附件对应驱动支持的条件参数定义列表.
     *
     * @param attachmentId 附件ID
     */
    Mono<List<AccessUrlCondition>> getUrlConditions(UUID attachmentId);
}
