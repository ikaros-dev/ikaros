package run.ikaros.server.core.subject.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.FileConst;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.server.core.subject.event.SubjectCoverImageDownloadAndUpdateEvent;
import run.ikaros.server.core.webclient.WeClientService;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Component
public class SubjectImageDownloadListener {
    private final WeClientService weClientService;
    private final SubjectRepository subjectRepository;

    public SubjectImageDownloadListener(WeClientService weClientService,
                                        SubjectRepository subjectRepository) {
        this.weClientService = weClientService;
        this.subjectRepository = subjectRepository;
    }


    /**
     * Handle image download and update subject image url.
     */
    @EventListener(SubjectCoverImageDownloadAndUpdateEvent.class)
    public Mono<Void> handle(SubjectCoverImageDownloadAndUpdateEvent event) {
        Long subjectId = event.getSubjectId();
        String coverUrl = event.getCoverUrl();
        Assert.hasText(coverUrl, "'coverUrl' must has text.");
        Assert.notNull(subjectId, "'subjectId' must not null.");
        return weClientService.downloadImageWithGet(FileConst.POLICY_LOCAL, coverUrl)
            .map(FileEntity::getUrl)
            .flatMap(url -> subjectRepository.findById(subjectId)
                .map(entity -> entity.setCover(url)))
            .flatMap(subjectRepository::save)
            .doOnSuccess(entity ->
                log.info("Update subject cover image entity for subject id={}, cover={}.",
                    subjectId, entity.getCover()))
            .then();
    }

}
