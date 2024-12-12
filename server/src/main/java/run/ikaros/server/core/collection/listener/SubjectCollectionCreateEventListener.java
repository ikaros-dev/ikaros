package run.ikaros.server.core.collection.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.collection.event.SubjectCollectionCreateEvent;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Component
public class SubjectCollectionCreateEventListener {
    private final EpisodeCollectionRepository episodeCollectionRepository;
    private final EpisodeRepository episodeRepository;

    public SubjectCollectionCreateEventListener(
        EpisodeCollectionRepository episodeCollectionRepository,
        EpisodeRepository episodeRepository) {
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
    }

    /**
     * 当条目收藏新增的时候，同步新增条目收藏下的所有剧集收藏.
     */
    @EventListener(SubjectCollectionCreateEvent.class)
    public Mono<Void> onSubjectCollectionCreateEvent(SubjectCollectionCreateEvent event) {
        Long subjectId = event.getSubjectId();
        Long userId = event.getUserId();
        log.debug("Receive SubjectCollectionCreateEvent for userId={} and subjectId={}",
            userId, subjectId);
        return episodeRepository.findAllBySubjectId(subjectId)
            .map(BaseEntity::getId)
            .flatMap(episodeId
                -> episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
                .switchIfEmpty(
                    episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                            .userId(userId)
                            .subjectId(subjectId)
                            .episodeId(episodeId)
                            .finish(false)
                            .build())
                        .doOnSuccess(entity -> log.debug(
                            "Create new episode collection "
                                + "for userId is [{}] and episode id is [{}]",
                            userId, entity.getEpisodeId()))))
            .then();
    }
}
