package run.ikaros.server.event;

import org.springframework.context.ApplicationEvent;

/**
 * @see run.ikaros.server.core.service.SubscribeService#subscribeBgmTvSubject(Long)
 */
public class SubscribeBgmTvSubjectEvent extends ApplicationEvent {

    private final Long bgmTvSubjectId;
    private final Long animeId;

    public SubscribeBgmTvSubjectEvent(Object source, Long bgmTvSubjectId, Long animeId) {
        super(source);
        this.bgmTvSubjectId = bgmTvSubjectId;
        this.animeId = animeId;
    }
}
