package run.ikaros.server.core.attachment.operator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverOperate;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;

@Slf4j
@Component
public class AttachmentDriverOperator implements AttachmentDriverOperate {
    private final AttachmentDriverService service;

    public AttachmentDriverOperator(AttachmentDriverService service) {
        this.service = service;
    }

    @Override
    public Mono<AttachmentDriver> save(AttachmentDriver driver) {
        return service.save(driver);
    }

    @Override
    public Mono<AttachmentDriver> findById(Long id) {
        return service.findById(id);
    }

    @Override
    public Mono<AttachmentDriver> findByTypeAndName(String type, String name) {
        return service.findByTypeAndName(type, name);
    }

    @Override
    public Mono<PagingWrap<Attachment>> listAttachmentsByCondition(
        AttachmentSearchCondition attachmentSearchCondition) {
        return service.listAttachmentsByCondition(attachmentSearchCondition);
    }

    @Override
    public Mono<Void> refresh(Long attachmentId) {
        return service.refresh(attachmentId);
    }

    @Override
    public Mono<PagingWrap<AttachmentDriver>> listDriversByCondition(Integer page,
                                                                     Integer pageSize) {
        return service.listDriversByCondition(page, pageSize);
    }
}
