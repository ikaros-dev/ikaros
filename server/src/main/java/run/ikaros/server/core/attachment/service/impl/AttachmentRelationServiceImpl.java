package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.constant.OpenApiConst.ATT_STREAM_ENDPOINT_PREFIX;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.cache.annotation.FluxCacheEvict;
import run.ikaros.server.cache.annotation.FluxCacheable;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
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


    /**
     * Construct.
     */
    public AttachmentRelationServiceImpl(
        AttachmentRelationRepository repository, AttachmentRepository attachmentRepository) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
    }


    @Override
    @FluxCacheable(value = "attachment:relations:", key = "#type + ' ' + #attachmentId")
    public Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                 UUID attachmentId) {
        Assert.notNull(type, "'type' must not null.");

        return repository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(attachmentRelationEntity -> copyProperties(
                attachmentRelationEntity, new AttachmentRelation()));
    }


    @Override
    @FluxCacheable(value = "attachment:video_subtitles:attachmentId:", key = "#attachmentId")
    public Flux<VideoSubtitle> findAttachmentVideoSubtitles(UUID attachmentId) {
        return repository.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId)
            .map(AttachmentRelationEntity::getRelationAttachmentId)
            .flatMap(attachmentRepository::findById)
            .map(attachmentEntity -> VideoSubtitle.builder()
                .masterAttachmentId(attachmentId)
                .attachmentId(attachmentEntity.getId())
                .name(attachmentEntity.getName())
                .url(ATT_STREAM_ENDPOINT_PREFIX + '/' + attachmentEntity.getId())
                .build());
    }

    @Override
    @MonoCacheEvict
    public Mono<AttachmentRelation> putAttachmentRelation(AttachmentRelation attachmentRelation) {
        Assert.notNull(attachmentRelation, "'attachmentRelation' must not be null.");
        UUID attachmentId = Objects.requireNonNull(attachmentRelation.getAttachmentId());
        AttachmentRelationType nullableType = attachmentRelation.getType();
        UUID relationAttachmentId =
            Objects.requireNonNull(attachmentRelation.getRelationAttachmentId());
        Assert.notNull(nullableType, "'type' must not null.");
        AttachmentRelationType type = Objects.requireNonNull(nullableType);
        return repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(type, attachmentId,
                relationAttachmentId)
            .filter(exist -> exist)
            .flatMap(
                e -> repository.findByTypeAndAttachmentIdAndRelationAttachmentId(type, attachmentId,
                    relationAttachmentId))
            .switchIfEmpty(Mono.defer(() -> repository.insert(AttachmentRelationEntity
                .builder().attachmentId(attachmentId).id(UuidV7Utils.generateUuid())
                .type(type).relationAttachmentId(relationAttachmentId)
                .build())))
            .flatMap(entity -> copyProperties(entity, attachmentRelation));
    }

    @Override
    @FluxCacheEvict
    public Flux<AttachmentRelation> putAttachmentRelations(
        PostAttachmentRelationsParam postAttachmentRelationsParam) {

        Assert.notNull(postAttachmentRelationsParam,
            "'postAttachmentRelationsParam' must not be null.");

        final UUID masterId = postAttachmentRelationsParam.getMasterId();
        final AttachmentRelationType type = postAttachmentRelationsParam.getType();
        final List<UUID> relationIds = postAttachmentRelationsParam.getRelationIds();

        if (masterId == null || type == null || relationIds == null) {
            throw new IllegalArgumentException(
                "'masterId', 'type' and 'relationIds' must not be null.");
        }
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
    @MonoCacheEvict
    public Mono<AttachmentRelation> deleteAttachmentRelation(
        AttachmentRelation attachmentRelation) {
        Assert.notNull(attachmentRelation, "'attachmentRelation' must not be null.");
        UUID id = attachmentRelation.getId();

        AttachmentRelationType nullableType = attachmentRelation.getType();
        UUID relationAttachmentId =
            Objects.requireNonNull(attachmentRelation.getRelationAttachmentId());
        UUID attachmentId = Objects.requireNonNull(attachmentRelation.getAttachmentId());
        Assert.notNull(nullableType, "'type' must not null.");
        AttachmentRelationType type = Objects.requireNonNull(nullableType);
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
