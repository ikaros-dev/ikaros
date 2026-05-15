package run.ikaros.server.core.episode;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.core.notify.NotifyService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Component
public class EpisodeAttachmentUpdateEventListener {
    private final AttachmentRepository attachmentRepository;
    private final EpisodeRepository episodeRepository;
    private final SubjectRepository subjectRepository;

    private final NotifyService notifyService;

    private final IkarosProperties ikarosProperties;

    /**
     * Construct.
     */
    public EpisodeAttachmentUpdateEventListener(AttachmentRepository attachmentRepository,
                                                EpisodeRepository episodeRepository,
                                                SubjectRepository subjectRepository,
                                                NotifyService notifyService,
                                                IkarosProperties ikarosProperties) {
        this.attachmentRepository = attachmentRepository;
        this.episodeRepository = episodeRepository;
        this.subjectRepository = subjectRepository;
        this.notifyService = notifyService;
        this.ikarosProperties = ikarosProperties;
    }

    /**
     * 如果需要发送通知则进行发送.
     */
    @EventListener(EpisodeAttachmentUpdateEvent.class)
    public Mono<Void> onEpisodeAttachmentUpdateEvent(EpisodeAttachmentUpdateEvent event) {
        log.debug("receive episode attachment update event: {}", event);
        if (!event.getNotify()) {
            return Mono.empty();
        }
        final UUID attachmentId = event.getAttachmentId();
        final UUID episodeId = event.getEpisodeId();
        return Mono.just(new Context())
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(context -> attachmentRepository.findById(attachmentId)
                .map(entity -> {
                    context.setVariable("attachment", entity);
                    return context;
                }))
            .flatMap(context -> episodeRepository.findById(episodeId)
                .map(entity -> {
                    String name = StringUtils.isEmpty(entity.getNameCn())
                        ? entity.getName() : entity.getNameCn();
                    context.setVariable("episodeName", name);
                    context.setVariable("episode", entity);
                    return context;
                }))
            .flatMap(context -> {
                Object episode = context.getVariable("episode");

                if (!(episode instanceof EpisodeEntity episodeEntity)) {
                    return Mono.empty();
                }
                UUID subjectId = episodeEntity.getSubjectId();
                return subjectRepository.findById(subjectId)
                    .map(entity -> {
                        String cover = entity.getCover();
                        if (StringUtils.isNotBlank(cover) && !cover.startsWith("http")) {
                            cover = ikarosProperties.getExternalUrl() + cover;
                        }
                        entity.setCover(cover);
                        String name = StringUtils.isBlank(entity.getNameCn())
                            ? entity.getName() : entity.getNameCn();
                        String subjectUrl = ikarosProperties.getExternalUrl()
                            + "/app/link/subject/" + subjectId;

                        context.setVariable("subjectName", name);
                        context.setVariable("subjectCover", cover);
                        context.setVariable("subject", entity);
                        context.setVariable("subjectUrl", subjectUrl);
                        return context;
                    });
            })
            .flatMap(context -> {
                Object episode = context.getVariable("episode");

                if (!(episode instanceof EpisodeEntity episodeEntity)) {
                    return Mono.empty();
                }
                Object attachment = context.getVariable("attachment");

                if (!(attachment instanceof AttachmentEntity attachmentEntity)) {
                    return Mono.empty();
                }

                Object subject = context.getVariable("subject");

                if (!(subject instanceof SubjectEntity subjectEntity)) {
                    return Mono.empty();
                }

                String sb = "番剧《"
                    + (StringUtils.isBlank(subjectEntity.getNameCn())
                    ? subjectEntity.getName() : subjectEntity.getNameCn())
                    + "》第"
                    + episodeEntity.getSequence()
                    + "集 ["
                    + (StringUtils.isBlank(episodeEntity.getNameCn())
                    ? episodeEntity.getName() : episodeEntity.getNameCn())
                    + "] 更新了";

                return notifyService.send(sb, "mail/anime_update", context);
            });
    }
}
