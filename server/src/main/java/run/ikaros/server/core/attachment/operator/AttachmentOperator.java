package run.ikaros.server.core.attachment.operator;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentOperate;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;

@Slf4j
@Component
public class AttachmentOperator implements AttachmentOperate {
    private final AttachmentService service;

    public AttachmentOperator(AttachmentService service) {
        this.service = service;
    }

    @Override
    public Mono<Attachment> save(Attachment attachment) {
        return service.save(attachment);
    }

    @Override
    public Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition searchCondition) {
        return service.listByCondition(searchCondition);
    }

    @Override
    public Mono<Void> removeById(Long attachmentId) {
        return service.removeById(attachmentId);
    }

    @Override
    public Mono<Attachment> upload(AttachmentUploadCondition uploadCondition) {
        return service.upload(uploadCondition);
    }

    @Override
    public Mono<Attachment> findById(Long attachmentId) {
        return service.findById(attachmentId);
    }

    @Override
    public Mono<Attachment> findByTypeAndParentIdAndName(AttachmentType type,
                                                         @Nullable Long parentId, String name) {
        return service.findByTypeAndParentIdAndName(type, parentId, name);
    }

    @Override
    public Mono<Void> removeByTypeAndParentIdAndName(AttachmentType type, @Nullable Long parentId,
                                                     String name) {
        return service.removeByTypeAndParentIdAndName(type, parentId, name);
    }

    @Override
    public Mono<Attachment> createDirectory(@Nullable Long parentId, String name) {
        return service.createDirectory(parentId, name);
    }

    @Override
    public Mono<Boolean> existsByParentIdAndName(@Nullable Long parentId, String name) {
        return service.existsByParentIdAndName(parentId, name);
    }

    @Override
    public Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type,
                                                        @Nullable Long parentId, String name) {
        return service.existsByTypeAndParentIdAndName(type, parentId, name);
    }
}
