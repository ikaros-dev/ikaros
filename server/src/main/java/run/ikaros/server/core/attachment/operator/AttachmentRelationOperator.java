package run.ikaros.server.core.attachment.operator;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.AttachmentRelationOperate;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;

@Slf4j
@Component
public class AttachmentRelationOperator implements AttachmentRelationOperate {
    private final AttachmentRelationService attachmentRelationService;

    public AttachmentRelationOperator(AttachmentRelationService attachmentRelationService) {
        this.attachmentRelationService = attachmentRelationService;
    }

    @Override
    public Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                 UUID attachmentId) {
        return attachmentRelationService.findAllByTypeAndAttachmentId(type, attachmentId);
    }
}
