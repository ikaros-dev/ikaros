package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.store.enums.AttachmentType.Driver_File;
import static run.ikaros.api.store.enums.AttachmentType.File;

import java.util.Objects;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.server.core.attachment.service.AttachmentContentInspectionService;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.service.ValidatedMediaStream;
import run.ikaros.server.store.entity.AttachmentEntity;

/** 默认附件有限前缀真实格式检查服务。 */
@Service
public class DefaultAttachmentContentInspectionService
    implements AttachmentContentInspectionService {

    /** 提供受路径校验保护的持久化附件内容流。 */
    private final AttachmentService attachmentService;
    /** 执行名称门禁和有限前缀真实格式检测。 */
    private final AttachmentMediaValidationService mediaValidationService;

    public DefaultAttachmentContentInspectionService(
        AttachmentService attachmentService,
        AttachmentMediaValidationService mediaValidationService) {
        this.attachmentService = attachmentService;
        this.mediaValidationService = mediaValidationService;
    }

    @Override
    public Mono<MediaFileDetectionResult> inspect(AttachmentEntity attachmentEntity) {
        Objects.requireNonNull(attachmentEntity, "attachmentEntity must not be null");
        if (attachmentEntity.getType() != File && attachmentEntity.getType() != Driver_File) {
            return Mono.empty();
        }
        return Mono.defer(() -> {
            mediaValidationService.validateFilename(attachmentEntity.getName());
            return attachmentService.getStreamByIdWithoutRange(attachmentEntity.getId())
                .flatMap(content -> inspect(content, attachmentEntity.getName()));
        });
    }

    @Override
    public Mono<MediaFileDetectionResult> inspect(Attachment attachment,
                                                  AttachmentDriverFetcher fetcher) {
        Objects.requireNonNull(attachment, "attachment must not be null");
        Objects.requireNonNull(fetcher, "fetcher must not be null");
        if (attachment.getType() != Driver_File) {
            return Mono.empty();
        }
        return Mono.defer(() -> {
            mediaValidationService.validateFilename(attachment.getName());
            return inspect(fetcher.getSteam(attachment), attachment.getName());
        });
    }

    private Mono<MediaFileDetectionResult> inspect(Flux<DataBuffer> content, String filename) {
        return mediaValidationService.validate(content, filename)
            .flatMap(this::releaseReplayPrefix);
    }

    private Mono<MediaFileDetectionResult> releaseReplayPrefix(ValidatedMediaStream validated) {
        return validated.content()
            .take(1)
            .doOnNext(DataBufferUtils::release)
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
            .then(Mono.just(validated.detectionResult()));
    }
}
