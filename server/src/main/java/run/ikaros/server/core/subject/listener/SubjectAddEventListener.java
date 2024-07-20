package run.ikaros.server.core.subject.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.core.subject.event.SubjectAddEvent;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Component
public class SubjectAddEventListener {
    private final EpisodeRepository episodeRepository;

    public SubjectAddEventListener(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    /**
     * Listen subject add event.
     */
    @EventListener(SubjectAddEvent.class)
    public Mono<Void> onSubjectAdd(SubjectAddEvent event) {
        SubjectEntity subjectEntity = event.getEntity();
        if (SubjectType.GAME.equals(subjectEntity.getType())) {
            return episodeRepository.findAllBySubjectId(subjectEntity.getId())
                .switchIfEmpty(episodeRepository.save(EpisodeEntity.builder()
                    .airTime(subjectEntity.getAirTime())
                    .subjectId(subjectEntity.getId())
                    .group(EpisodeGroup.MAIN)
                    .sequence(0)
                    .name("Game archive package")
                    .nameCn("游戏本体归档包")
                    .build())).then();
        }
        return Mono.empty();
    }

}
