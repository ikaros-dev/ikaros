package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.server.store.entity.AttachmentEntity;

/** 提供持久化附件和驱动扫描结果的有限前缀真实格式检查. */
public interface AttachmentContentInspectionService {

    /**
     * 重新检查持久化附件的真实媒体格式.
     *
     * @param attachmentEntity 待检查的附件实体
     * @return 真实媒体格式；目录等非普通文件不会返回结果
     */
    Mono<MediaFileDetectionResult> inspect(AttachmentEntity attachmentEntity);

    /**
     * 使用已选驱动读取扫描结果并检查真实媒体格式.
     *
     * @param attachment 待检查的驱动扫描结果
     * @param fetcher 已选中的附件驱动读取器
     * @return 真实媒体格式；目录等非普通文件不会返回结果
     */
    Mono<MediaFileDetectionResult> inspect(Attachment attachment,
                                           AttachmentDriverFetcher fetcher);
}
