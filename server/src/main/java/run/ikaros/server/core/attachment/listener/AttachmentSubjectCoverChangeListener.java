package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.subject.SubjectOperator;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.core.subject.event.SubjectUpdateEvent;
import run.ikaros.server.infra.utils.ByteArrUtils;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Component
public class AttachmentSubjectCoverChangeListener {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SubjectRepository subjectRepository;

    /**
     * Construct.
     */
    public AttachmentSubjectCoverChangeListener(
        AttachmentRepository attachmentRepository,
        AttachmentService attachmentService,
        AttachmentReferenceRepository attachmentReferenceRepository,
        SubjectOperator subjectOperator, SubjectRepository subjectRepository) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.subjectRepository = subjectRepository;
    }


    private Mono<Void> deleteAttachmentByCover(String cover) {
        if (StringUtils.isBlank(cover)) {
            return Mono.empty();
        }

        return attachmentRepository.findByUrl(cover)
            .map(AttachmentEntity::getId)
            .flatMap(attachmentService::removeByIdForcibly);
    }

    /**
     * Listen subject remove event.
     */
    @EventListener(SubjectRemoveEvent.class)
    public Mono<Void> onSubjectRemove(SubjectRemoveEvent event) {
        SubjectEntity subjectEntity = event.getEntity();
        UUID subjectId = subjectEntity.getId();
        String cover = subjectEntity.getCover();
        if (Objects.isNull(subjectId) || StringUtils.isBlank(cover)) {
            return Mono.empty();
        }
        return deleteAttachmentByCover(cover);
    }

    /**
     * Listen subject update event.
     */
    @EventListener(SubjectUpdateEvent.class)
    public Mono<Void> onSubjectCoverUpdate(SubjectUpdateEvent event) {
        SubjectEntity oldEntity = event.getOldEntity();
        SubjectEntity newEntity = event.getNewEntity();
        if (Objects.isNull(oldEntity) || Objects.isNull(newEntity)) {
            return Mono.empty();
        }

        String oldCover = oldEntity.getCover();
        String newCover = newEntity.getCover();
        if (oldCover.equals(newCover) && !oldCover.startsWith("http")) {
            return Mono.empty();
        }

        return attachmentRepository.findByUrl(oldCover)
            .map(AttachmentEntity::getId)
            .flatMap(oldCoverAttId -> attachmentReferenceRepository
                .deleteByTypeAndAttachmentIdAndReferenceId(
                    AttachmentReferenceType.SUBJECT,
                    oldEntity.getId(),
                    oldCoverAttId
                ).doOnSuccess(unused ->
                    log.debug("Delete attachment Reference by type and att id and sub id.")))

            // 当是网络url的时候，附件是找不到的，此时为空走这里的逻辑
            // 条目三方同步会发布更新事件
            .then(Mono.just(newCover))
            .filter(StringUtils::isNotBlank)
            .filter(url -> url.startsWith("http"))
            .publishOn(Schedulers.boundedElastic())
            .flatMap(url -> {
                byte[] bytes = restTemplate.getForObject(url, byte[].class);
                if (bytes == null || !ByteArrUtils.isBinaryData(bytes)) {
                    log.warn("Download subject cover fail for url: {}", url);
                    if (ByteArrUtils.isStringData(bytes)) {
                        log.warn("Response: {}", new String(bytes, StandardCharsets.UTF_8));
                    }
                    return Mono.empty();
                }
                DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                return attachmentService.upload(AttachmentUploadCondition.builder()
                    .parentId(COVER_DIRECTORY_ID)
                    .name(getCoverName(newEntity))
                    .dataBufferFlux(Mono.just(dataBufferFactory.wrap(bytes)).flux())
                    .build());
            })
            .flatMap(attachment ->
                subjectRepository.findByNsfwAndTypeAndNameAndSummary(
                        newEntity.getNsfw(), newEntity.getType(),
                        newEntity.getName(), newEntity.getSummary())
                    .map(entity -> entity.setCover(attachment.getUrl()))
                    .flatMap(subjectRepository::save)
                    .flatMap(entity ->
                        attachmentReferenceRepository.findByTypeAndAttachmentIdAndReferenceId(
                                AttachmentReferenceType.SUBJECT, attachment.getId(), entity.getId())
                            .switchIfEmpty(Mono.just(AttachmentReferenceEntity.builder()
                                .type(AttachmentReferenceType.SUBJECT)
                                .attachmentId(attachment.getId())
                                .referenceId(entity.getId())
                                .build()))
                            .flatMap(attachmentReferenceRepository::save)
                    )

            )

            .then(moveCover2CoverDir(newEntity))

            .then();
    }

    private String getCoverName(SubjectEntity subjectEntity) {
        final String url = subjectEntity.getCover();
        String coverFileName = StringUtils.isNotBlank(subjectEntity.getNameCn())
            ? subjectEntity.getNameCn() : subjectEntity.getName();
        coverFileName =
            System.currentTimeMillis() + "-" + coverFileName
                + "." + FileUtils.parseFilePostfix(FileUtils.parseFileName(url));
        return coverFileName;
    }

    /**
     * update new attachment that move to cover dir.
     */
    private Mono<AttachmentEntity> moveCover2CoverDir(SubjectEntity newEntity) {
        return attachmentRepository.findByUrl(newEntity.getCover())
            .filter(entity -> !entity.getParentId().equals(COVER_DIRECTORY_ID))
            .map(entity -> entity.setParentId(COVER_DIRECTORY_ID)
                .setName(getCoverName(newEntity)))
            .flatMap(attachmentRepository::save);
    }
}

