package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_URL_SPLIT_STR;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;
import static run.ikaros.api.store.enums.AttachmentDriverType.LOCAL;

import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.cache.annotation.FluxCacheEvict;
import run.ikaros.server.cache.annotation.FluxCacheable;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentRelationEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentRelationServiceImpl implements AttachmentRelationService {
    private final AttachmentRelationRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentDriverRepository driverRepository;


    /**
     * Construct.
     */
    public AttachmentRelationServiceImpl(
        AttachmentRelationRepository repository, AttachmentRepository attachmentRepository,
        AttachmentDriverRepository driverRepository) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
        this.driverRepository = driverRepository;
    }


    @Override
    @FluxCacheable(value = "attachment:relations:", key = "#type + ' ' + #attachmentId")
    public Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                 Long attachmentId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");

        return repository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(attachmentRelationEntity -> copyProperties(
                attachmentRelationEntity, new AttachmentRelation()));
    }

    /**
     * 返回结果时转化下URL
     * .
     */
    private Mono<AttachmentEntity> convertDriverUrlWhenTypeStartWithDriver(
        AttachmentEntity attachment) {
        AttachmentType type = attachment.getType();
        final String oldUrl = attachment.getUrl();
        if (type == null || oldUrl == null || !oldUrl.contains(DRIVER_URL_SPLIT_STR)) {
            return Mono.just(attachment);
        }

        if (type.toString().toUpperCase(Locale.ROOT).startsWith("DRIVER")) {
            Long driverId = Long.valueOf(oldUrl.split(AttachmentConst.DRIVER_URL_SPLIT_STR)[0]);
            final String newUrl = DRIVER_STATIC_RESOURCE_PREFIX + attachment.getPath();
            return driverRepository.findById(driverId)
                .filter(driver -> LOCAL.equals(driver.getType()))
                .map(driver -> attachment.setUrl(newUrl));
        }
        return Mono.just(attachment);
    }


    @Override
    @FluxCacheable(value = "attachment:video_subtitles:attachmentId:", key = "#attachmentId")
    public Flux<VideoSubtitle> findAttachmentVideoSubtitles(Long attachmentId) {
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        return repository.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId)
            .map(AttachmentRelationEntity::getRelationAttachmentId)
            .flatMap(attachmentRepository::findById)
            .flatMap(this::convertDriverUrlWhenTypeStartWithDriver)
            .map(attachmentEntity -> VideoSubtitle.builder()
                .masterAttachmentId(attachmentId)
                .attachmentId(attachmentEntity.getId())
                .name(attachmentEntity.getName())
                .url(attachmentEntity.getUrl())
                .build());
    }

    @Override
    @MonoCacheEvict
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
    @FluxCacheEvict
    public Flux<AttachmentRelation> putAttachmentRelations(
        PostAttachmentRelationsParam postAttachmentRelationsParam) {

        Assert.notNull(postAttachmentRelationsParam,
            "'postAttachmentRelationsParam' must not be null.");

        final Long masterId = postAttachmentRelationsParam.getMasterId();
        final AttachmentRelationType type = postAttachmentRelationsParam.getType();
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
    @MonoCacheEvict
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
