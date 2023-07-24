package run.ikaros.server.core.notify.listener;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.core.file.FileService;
import run.ikaros.server.core.notify.MailService;
import run.ikaros.server.core.notify.model.MailRequest;
import run.ikaros.server.core.subject.event.EpisodeFileUpdateEvent;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.infra.constants.ThymeleafConst;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Component
public class EpisodeFileUpdateEventListener {
    private final MailService mailService;
    private final EpisodeRepository episodeRepository;
    private final SubjectService subjectService;
    private final FileService fileService;
    private final IkarosProperties ikarosProperties;

    /**
     * Construct.
     */
    public EpisodeFileUpdateEventListener(MailService mailService,
                                          EpisodeRepository episodeRepository,
                                          SubjectService subjectService, FileService fileService,
                                          IkarosProperties ikarosProperties) {
        this.mailService = mailService;
        this.episodeRepository = episodeRepository;
        this.subjectService = subjectService;
        this.fileService = fileService;
        this.ikarosProperties = ikarosProperties;
    }

    /**
     * Send mail to user.
     */
    @EventListener(EpisodeFileUpdateEvent.class)
    public Mono<Void> handle(EpisodeFileUpdateEvent event) {
        if (Objects.isNull(event)) {
            return Mono.empty();
        }
        if (!event.getNotify() || !mailService.getMailConfig().getEnable()) {
            return Mono.empty();
        }
        final Long episodeId = event.getEpisodeId();
        final Long fileId = event.getFileId();
        final var context = new Context();
        MailRequest mailRequest = new MailRequest();
        mailRequest.setAddress(null);
        return episodeRepository.findById(episodeId)
            .doOnNext(entity -> {
                context.setVariable("epTitle",
                    StringUtils.isNotBlank(entity.getNameCn()) ? entity.getNameCn() :
                        entity.getName());
                context.setVariable("epSeq", entity.getSequence());
                context.setVariable("epIntroduction", entity.getDescription());
                mailRequest.setTitle("第" + entity.getSequence() + "集更新");
            })
            .map(EpisodeEntity::getSubjectId)
            .flatMap(subjectService::findById)
            .doOnNext(subject -> {
                String title = StringUtils.isNotBlank(subject.getNameCn()) ? subject.getNameCn() :
                    subject.getName();
                context.setVariable("title", title);
                context.setVariable("introduction", subject.getSummary());
                context.setVariable("coverImgUrl",
                    ikarosProperties.getExternalUrl() + subject.getCover());
                String oldReqTitle = mailRequest.getTitle();
                mailRequest.setTitle("番剧《" + title + "》" + oldReqTitle);
            })
            .then(fileService.findById(fileId))
            .doOnNext(fileEntity -> context.setVariable("epUrlFileName", fileEntity.getName()))
            .then(Mono.just(mailService))
            .flatMap(mailService1 -> mailService1.send(mailRequest, ThymeleafConst.ANIME_UPDATE,
                context))
            .doOnSuccess(unused -> log.info("send mail success."))
            .then();
    }
}
