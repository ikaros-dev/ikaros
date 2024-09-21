package run.ikaros.server.core.attachment.listener;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.core.subject.event.SubjectUpdateEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentSubjectCoverChangeListener {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final AttachmentReferenceRepository attachmentReferenceRepository;

    /**
     * Construct.
     */
    public AttachmentSubjectCoverChangeListener(
        AttachmentRepository attachmentRepository,
        AttachmentService attachmentService,
        AttachmentReferenceRepository attachmentReferenceRepository) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
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
        Long subjectId = subjectEntity.getId();
        String cover = subjectEntity.getCover();
        if (Objects.isNull(subjectId) || subjectId < 0 || StringUtils.isBlank(cover)) {
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
        if (oldCover.equals(newCover)) {
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
            // update new attachment that move to cover dir
            .then(attachmentRepository.findByTypeAndParentIdAndName(AttachmentType.Directory,
                AttachmentConst.ROOT_DIRECTORY_ID, AttachmentConst.COVER_DIR_NAME))
            .map(AttachmentEntity::getId)
            .flatMap(coverDirAttId -> attachmentRepository.findByUrl(newCover)
                .filter(entity -> !coverDirAttId.equals(entity.getParentId()))
                .map(entity -> entity.setParentId(coverDirAttId)))
            .flatMap(attachmentRepository::save)
            .map(AttachmentEntity::getId)
            .flatMap(attId -> attachmentReferenceRepository
                .findByTypeAndAttachmentIdAndReferenceId(AttachmentReferenceType.SUBJECT,
                    attId, newEntity.getId())
                .switchIfEmpty(attachmentReferenceRepository.save(
                    AttachmentReferenceEntity.builder()
                        .type(AttachmentReferenceType.SUBJECT)
                        .attachmentId(attId)
                        .referenceId(newEntity.getId())
                        .build()
                ).doOnSuccess(entity ->
                    log.debug("Create attachment Reference by type and att id and sub id: [{}].",
                        entity)))
            )

            .then();
    }
}
