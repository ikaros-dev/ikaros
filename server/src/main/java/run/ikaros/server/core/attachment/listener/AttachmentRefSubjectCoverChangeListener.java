package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.exception.AttachmentRemoveException;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.subject.event.SubjectAddEvent;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentRefSubjectCoverChangeListener {
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final AttachmentRepository attachmentRepository;

    public AttachmentRefSubjectCoverChangeListener(
        AttachmentReferenceRepository attachmentReferenceRepository,
        AttachmentRepository attachmentRepository) {
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Construct.
     */
    @EventListener(SubjectAddEvent.class)
    public Mono<Void> onSubjectAdd(SubjectAddEvent event) {
        SubjectEntity subjectEntity = event.getEntity();
        Long subjectId = subjectEntity.getId();
        String cover = subjectEntity.getCover();
        if (Objects.isNull(subjectId) || subjectId < 0 || StringUtils.isBlank(cover)) {
            return Mono.empty();
        }
        return attachmentRepository.findByUrl(cover)
            .map(attachmentEntity -> attachmentEntity.setParentId(COVER_DIRECTORY_ID))
            // 文件名称加上 当前时间戳 - 条目原始名称 - 条目放送日期时间戳 - 封面文件原始名称
            .map(entity -> entity.setName(
                System.currentTimeMillis()
                    + "-" + subjectEntity.getName()
                    + (Objects.nonNull(subjectEntity.getAirTime())
                    ? ("-" + subjectEntity.getAirTime()
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli())
                    : "")
                    + "-" + entity.getName()))
            .flatMap(attachmentRepository::save)
            .map(AttachmentEntity::getId)
            .map(attId -> AttachmentReferenceEntity.builder()
                .type(AttachmentReferenceType.SUBJECT)
                .attachmentId(attId)
                .referenceId(subjectId)
                .build())
            .flatMap(attachmentReferenceRepository::save)
            .then();
    }

    /**
     * Construct.
     */
    @EventListener(SubjectRemoveEvent.class)
    public Mono<Void> onSubjectRemove(SubjectRemoveEvent event) {
        SubjectEntity subjectEntity = event.getEntity();
        Long subjectId = subjectEntity.getId();
        String cover = subjectEntity.getCover();
        if (Objects.isNull(subjectId) || subjectId < 0 || StringUtils.isBlank(cover)) {
            return Mono.empty();
        }
        return attachmentRepository.findByUrl(cover)
            .<Long>handle((attachmentEntity, sink) -> {
                String fsPath = attachmentEntity.getFsPath();
                try {
                    Files.deleteIfExists(Path.of(fsPath));
                } catch (IOException e) {
                    sink.error(new AttachmentRemoveException(
                        "Delete attachment fail when delete fs file by fsPath=" + fsPath, e));
                    return;
                }
                sink.next(attachmentEntity.getId());
            })
            .flatMap(attId -> attachmentRepository.deleteById(attId).then(Mono.just(attId)))
            .flatMap(attId -> attachmentReferenceRepository
                .findByTypeAndAttachmentIdAndReferenceId(AttachmentReferenceType.SUBJECT,
                    attId, subjectId))
            .flatMap(attachmentReferenceRepository::delete)
            .then();
    }
}
