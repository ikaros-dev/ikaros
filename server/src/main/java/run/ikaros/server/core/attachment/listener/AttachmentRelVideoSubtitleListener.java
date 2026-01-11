package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.store.enums.AttachmentType.Driver_File;
import static run.ikaros.api.store.enums.AttachmentType.File;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.AttachmentRelationEntity;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentRelVideoSubtitleListener {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;

    public AttachmentRelVideoSubtitleListener(
        AttachmentRepository attachmentRepository,
        AttachmentRelationRepository attachmentRelationRepository) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentRelationRepository = attachmentRelationRepository;
    }

    /**
     * 监听附件关系保存事件{@link AttachmentReferenceSaveEvent},
     * 如果保存的类型是剧集引用，则查询一次对应的附件是否存在同名字幕，
     * 如果存在则在附件关系表新增一条视频字幕类型的关系记录.
     */
    @EventListener(AttachmentReferenceSaveEvent.class)
    public Mono<Void> onAttachmentReferenceSaveEvent(AttachmentReferenceSaveEvent event) {
        AttachmentReferenceEntity entity = event.getEntity();
        if (!AttachmentReferenceType.EPISODE.equals(entity.getType())) {
            return Mono.empty();
        }
        UUID attachmentId = entity.getAttachmentId();

        return findAttachmentSubtitlesAndSaveRelationIfNotExists(attachmentId);
    }


    /**
     * 监听剧集资源匹配更新事件{@link EpisodeAttachmentUpdateEvent}, 查询对应的剧集附件，
     * 在数据库中，是否存在相同名称的字幕文件，
     * 如果存在则新增一条附件间关系{@link run.ikaros.api.store.enums.AttachmentRelationType#VIDEO_SUBTITLE}.
     */
    @EventListener(EpisodeAttachmentUpdateEvent.class)
    public Mono<Void> onSubjectAdd(EpisodeAttachmentUpdateEvent event) {
        UUID attachmentId = event.getAttachmentId();
        return findAttachmentSubtitlesAndSaveRelationIfNotExists(attachmentId);
    }

    private Mono<Void> findAttachmentSubtitlesAndSaveRelationIfNotExists(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
            .flatMapMany(this::findAllAttachmentSubtitles)
            .map(VideoSubtitle::getAttachmentId)
            .flatMap(relationAttId -> attachmentRelationRepository
                .existsByTypeAndAttachmentIdAndRelationAttachmentId(
                    AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttId)
                .filter(exists -> !exists)
                .map(exists -> AttachmentRelationEntity.builder()
                    .id(UuidV7Utils.generateUuid())
                    .type(AttachmentRelationType.VIDEO_SUBTITLE)
                    .attachmentId(attachmentId)
                    .relationAttachmentId(relationAttId)
                    .build())
                .flatMap(attachmentRelationEntity -> attachmentRelationRepository
                    .insert(attachmentRelationEntity)
                    .doOnSuccess(entity -> log.debug("Save new attachment relation record"
                            + " for type={} attId={} relAttId={}",
                        AttachmentRelationType.VIDEO_SUBTITLE,
                        attachmentId, relationAttId))))
            .then();
    }

    private Flux<VideoSubtitle> findAllAttachmentSubtitles(AttachmentEntity attachmentEntity) {
        String attachmentName = attachmentEntity.getName();
        String postfix = FileUtils.parseFilePostfix(attachmentName);
        attachmentName = attachmentName.substring(0, attachmentName.indexOf(postfix));
        return attachmentRepository.findAllByTypeAndNameLike(Driver_File, attachmentName + "%")
            .filter(entity ->
                (entity.getName().endsWith("ass") || entity.getName().endsWith("ssa")))
            .switchIfEmpty(attachmentRepository.findAllByTypeAndNameLike(File,
                    attachmentName + "%")
                .filter(entity ->
                    (entity.getName().endsWith("ass") || entity.getName().endsWith("ssa"))))
            .map(entity -> VideoSubtitle.builder()
                .masterAttachmentId(attachmentEntity.getId())
                .attachmentId(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .build());
    }

}
