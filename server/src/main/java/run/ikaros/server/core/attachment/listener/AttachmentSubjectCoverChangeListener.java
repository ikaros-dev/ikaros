package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.SsrfUtils;
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
    private final RestTemplate restTemplate = createRestTemplate();
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

    private static RestTemplate createRestTemplate() {
        return new RestTemplate(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        });
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

        UUID oldSubjectId = oldEntity.getId();
        if (oldSubjectId == null) {
            return Mono.empty();
        }
        return attachmentRepository.findByUrl(oldCover)
            .flatMap(oldCoverAttachment -> {
                UUID oldCoverAttId = oldCoverAttachment.getId();
                if (oldCoverAttId == null) {
                    return Mono.empty();
                }
                return attachmentReferenceRepository
                .deleteByTypeAndAttachmentIdAndReferenceId(
                    AttachmentReferenceType.SUBJECT,
                    oldSubjectId,
                    oldCoverAttId
                ).doOnSuccess(unused ->
                    log.debug("Delete attachment Reference by type and att id and sub id."));
            })

            // 当是网络url的时候，附件是找不到的，此时为空走这里的逻辑
            // 条目三方同步会发布更新事件
            .then(Mono.just(newCover))
            .filter(StringUtils::isNotBlank)
            .filter(url -> SsrfUtils.isSafeUrl(url))
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
            .flatMap(attachment -> {
                String attachmentUrl = attachment.getUrl();
                UUID attachmentId = attachment.getId();
                if (attachmentUrl == null || attachmentId == null) {
                    return Mono.empty();
                }
                return subjectRepository.findByNsfwAndTypeAndNameAndSummary(
                        newEntity.getNsfw(), newEntity.getType(),
                        newEntity.getName(), newEntity.getSummary())
                    .map(entity -> entity.setCover(attachmentUrl))
                    .flatMap(subjectRepository::update)
                    .flatMap(entity -> {
                        UUID subjectId = entity.getId();
                        if (subjectId == null) {
                            return Mono.empty();
                        }
                        return
                        attachmentReferenceRepository.findByTypeAndAttachmentIdAndReferenceId(
                                AttachmentReferenceType.SUBJECT, attachmentId, subjectId)
                            .flatMap(attachmentReferenceRepository::update)
                            .switchIfEmpty(attachmentReferenceRepository.insert(
                                AttachmentReferenceEntity.builder()
                                .type(AttachmentReferenceType.SUBJECT)
                                .attachmentId(attachmentId)
                                .referenceId(subjectId)
                                .build()));
                    });
            })

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
            .filter(entity -> !COVER_DIRECTORY_ID.equals(entity.getParentId()))
            .map(entity -> entity.setParentId(COVER_DIRECTORY_ID)
                .setName(getCoverName(newEntity)))
            .flatMap(attachmentRepository::update);
    }
}

