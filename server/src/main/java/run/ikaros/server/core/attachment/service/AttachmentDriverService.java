package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.wrap.PagingWrap;

public interface AttachmentDriverService {

    Mono<AttachmentDriver> save(AttachmentDriver driver);

    Mono<Void> removeById(Long id);

    Mono<Void> removeByTypeAndName(String type, String name);

    Mono<AttachmentDriver> findById(Long id);

    Mono<AttachmentDriver> findByTypeAndName(String type, String name);

    Mono<Void> enable(Long driverId);

    Mono<Void> disable(Long driverId);

    Mono<PagingWrap<Attachment>> listAttachmentsByCondition(
        AttachmentSearchCondition attachmentSearchCondition);

    Mono<Void> refresh(Long attachmentId);

    Mono<PagingWrap<AttachmentDriver>> listDriversByCondition(
        Integer page, Integer pageSize
    );
}
