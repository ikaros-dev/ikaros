package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;
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
            .flatMap(attachmentRelationEntity -> copyProperties(
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
                .masterAttachmentId(attachmentId)
                .attachmentId(attachmentEntity.getId())
                .name(attachmentEntity.getName())
                .url(attachmentEntity.getUrl())
                .build());
    }

    @Override
    public Mono<AttachmentRelation> putAttachmentRelation(AttachmentRelation attachmentRelation) {
        Assert.notNull(attachmentRelation, "'attachmentRelation' must not be null.");
        Long attachmentId = attachmentRelation.getAttachmentId();
        AttachmentRelationType type = attachmentRelation.getType();
        Long relationAttachmentId = attachmentRelation.getRelationAttachmentId();
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        Assert.isTrue(relationAttachmentId > 0, "'relationAttachmentId' must > 0.");
        Assert.notNull(type, "'type' must not null.");
        return repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(type, attachmentId,
                relationAttachmentId)
            .filter(exist -> exist)
            .flatMap(
                e -> repository.findByTypeAndAttachmentIdAndRelationAttachmentId(type, attachmentId,
                    relationAttachmentId))
            .switchIfEmpty(repository.save(AttachmentRelationEntity
                .builder().attachmentId(attachmentId)
                .type(type).relationAttachmentId(relationAttachmentId)
                .build()))
            .flatMap(entity -> copyProperties(entity, attachmentRelation));
    }

    @Override
    public Flux<AttachmentRelation> putAttachmentRelations(
        PostAttachmentRelationsParam postAttachmentRelationsParam) {

        Assert.notNull(postAttachmentRelationsParam,
            "'postAttachmentRelationsParam' must not be null.");

        final Long masterId = postAttachmentRelationsParam.getMasterId();
        final  AttachmentRelationType type = postAttachmentRelationsParam.getType();
        final List<Long> relationIds = postAttachmentRelationsParam.getRelationIds();

        Assert.isTrue(masterId > 0, "'masterId' must > 0.");
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(!relationIds.isEmpty(),
            "'relationIds' must not empty.");

        return Flux.fromStream(relationIds.stream())
            .map(relationId -> AttachmentRelation.builder()
                .attachmentId(masterId)
                .type(type)
                .relationAttachmentId(relationId)
                .build())
            .flatMap(this::putAttachmentRelation);
    }

    @Override
    public Mono<AttachmentRelation> deleteAttachmentRelation(
        AttachmentRelation attachmentRelation) {
        Assert.notNull(attachmentRelation, "'attachmentRelation' must not be null.");
        Long id = attachmentRelation.getId();
        if (id != null && id > 0) {
            return repository.deleteById(id)
                .then(Mono.just(attachmentRelation));
        }

        AttachmentRelationType type = attachmentRelation.getType();
        Long relationAttachmentId = attachmentRelation.getRelationAttachmentId();
        Long attachmentId = attachmentRelation.getAttachmentId();
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        Assert.isTrue(relationAttachmentId > 0, "'relationAttachmentId' must > 0.");
        Assert.notNull(type, "'type' must not null.");
        return repository
            .existsByTypeAndAttachmentIdAndRelationAttachmentId(
                type, attachmentId, relationAttachmentId)
            .filter(exist -> exist)
            .switchIfEmpty(Mono.error(
                new NotFoundException("Not found attachment relation for type="
                    + type + " and attachmentId=" + attachmentId
                    + " and relationId=" + relationAttachmentId)))
            .flatMap(ex -> repository.deleteByTypeAndAttachmentIdAndRelationAttachmentId(
                type, attachmentId, relationAttachmentId
            ))
            .then(Mono.just(attachmentRelation));
    }

}
