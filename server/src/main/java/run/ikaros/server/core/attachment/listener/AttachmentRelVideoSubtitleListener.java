package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.store.enums.AttachmentType.Driver_File;
import static run.ikaros.api.store.enums.AttachmentType.File;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.core.media.MediaFileCategory;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.api.core.media.MediaFileFormat;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.core.attachment.service.AttachmentContentInspectionService;
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
    /** 对视频和字幕候选执行有限前缀真实格式检查. */
    private final AttachmentContentInspectionService contentInspectionService;

    /**
     * 创建视频字幕附件关系监听器.
     *
     * @param attachmentRepository 附件仓库
     * @param attachmentRelationRepository 附件关系仓库
     * @param contentInspectionService 附件内容检查服务
     */
    public AttachmentRelVideoSubtitleListener(
        AttachmentRepository attachmentRepository,
        AttachmentRelationRepository attachmentRelationRepository,
        AttachmentContentInspectionService contentInspectionService) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentRelationRepository = attachmentRelationRepository;
        this.contentInspectionService = contentInspectionService;
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
            .flatMapMany(attachment -> contentInspectionService.inspect(attachment)
                .filter(result -> result.category() == MediaFileCategory.VIDEO)
                .flatMapMany(ignored -> findAllAttachmentSubtitles(attachment))
                .onErrorResume(exception -> {
                    log.debug("Skip subtitle relation for invalid video attachment: {}, reason={}",
                        attachment.getId(), exception.getClass().getSimpleName());
                    return Flux.empty();
                }))
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
        String videoBaseName = baseName(attachmentEntity.getName());
        Flux<Map.Entry<AttachmentEntity, MediaFileDetectionResult>> inspectedCandidates =
            inspectSubtitleCandidates(attachmentRepository.findAllByTypeAndNameLike(
                Driver_File, videoBaseName + "%"))
                .switchIfEmpty(inspectSubtitleCandidates(
                    attachmentRepository.findAllByTypeAndNameLike(
                        File, videoBaseName + "%")));
        return inspectedCandidates
            .collectList()
            .flatMapMany(inspected -> Flux.fromIterable(inspected)
                .filter(entry -> matchesVideoBaseName(videoBaseName, entry.getKey().getName()))
                .filter(entry -> hasRequiredVobSubPair(entry, inspected))
                .map(entry -> toVideoSubtitle(attachmentEntity, entry.getKey())));
    }

    private Flux<Map.Entry<AttachmentEntity, MediaFileDetectionResult>> inspectSubtitleCandidates(
        Flux<AttachmentEntity> candidates) {
        return candidates
            .filter(entity -> MediaFilePolicy.isAllowedFileName(entity.getName()))
            .concatMap(entity -> contentInspectionService.inspect(entity)
                .filter(result -> result.category() == MediaFileCategory.SUBTITLE)
                .map(result -> Map.entry(entity, result))
                .onErrorResume(exception -> {
                    log.debug("Skip invalid subtitle attachment: {}, reason={}",
                        entity.getId(), exception.getClass().getSimpleName());
                    return Mono.empty();
                }));
    }

    private boolean hasRequiredVobSubPair(
        Map.Entry<AttachmentEntity, MediaFileDetectionResult> candidate,
        List<Map.Entry<AttachmentEntity, MediaFileDetectionResult>> inspected) {
        MediaFileFormat format = candidate.getValue().format();
        if (format != MediaFileFormat.IDX && format != MediaFileFormat.VOBSUB) {
            return true;
        }
        MediaFileFormat pairedFormat = format == MediaFileFormat.IDX
            ? MediaFileFormat.VOBSUB : MediaFileFormat.IDX;
        String candidateBaseName = baseName(candidate.getKey().getName());
        return inspected.stream().anyMatch(entry -> entry.getValue().format() == pairedFormat
            && candidateBaseName.equals(baseName(entry.getKey().getName())));
    }

    private boolean matchesVideoBaseName(String videoBaseName, String subtitleName) {
        String subtitleBaseName = baseName(subtitleName);
        return subtitleBaseName.equals(videoBaseName)
            || subtitleBaseName.startsWith(videoBaseName + ".");
    }

    private String baseName(String filename) {
        return MediaFilePolicy.extractExtension(filename)
            .map(extension -> filename.substring(0, filename.length() - extension.length() - 1))
            .orElse("");
    }

    private VideoSubtitle toVideoSubtitle(AttachmentEntity master,
                                          AttachmentEntity subtitle) {
        return VideoSubtitle.builder()
            .masterAttachmentId(master.getId())
            .attachmentId(subtitle.getId())
            .name(subtitle.getName())
            .url(subtitle.getUrl())
            .build();
    }

}
