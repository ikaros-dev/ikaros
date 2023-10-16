package run.ikaros.server.core.attachment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.repository.AttachmentRelationRepository;

@Slf4j
@Service
public class AttachmentRelationServiceImpl implements AttachmentRelationService {
    private final AttachmentRelationRepository attachmentRelationRepository;

    public AttachmentRelationServiceImpl(
        AttachmentRelationRepository attachmentRelationRepository) {
        this.attachmentRelationRepository = attachmentRelationRepository;
    }


    @Override
    public Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                 Long attachmentId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");

        return attachmentRelationRepository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(attachmentRelationEntity -> ReactiveBeanUtils.copyProperties(
                attachmentRelationEntity, new AttachmentRelation()));
    }

}
