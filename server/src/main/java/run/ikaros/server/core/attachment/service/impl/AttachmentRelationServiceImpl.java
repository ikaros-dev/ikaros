package run.ikaros.server.core.attachment.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.AttachmentRelationEntity;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentRelationServiceImpl implements AttachmentRelationService {
    private final AttachmentRelationRepository repository;
    private final AttachmentRepository attachmentRepository;


    public AttachmentRelationServiceImpl(
        AttachmentRelationRepository repository, AttachmentRepository attachmentRepository) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
    }


    @Override
    public Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                 Long attachmentId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");

        return repository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(attachmentRelationEntity -> ReactiveBeanUtils.copyProperties(
                attachmentRelationEntity, new AttachmentRelation()));
    }

    @Override
    public Flux<VideoSubtitle> findAttachmentVideoSubtitles(Long attachmentId) {
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        return repository.findAllByTypeAndAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId)
            .map(AttachmentRelationEntity::getRelationAttachmentId)
            .flatMap(attachmentRepository::findById)
            .map(attachmentEntity -> VideoSubtitle.builder()
                .attachmentId(attachmentEntity.getId())
                .name(attachmentEntity.getName())
                .url(attachmentEntity.getUrl())
                .build());
    }

}
