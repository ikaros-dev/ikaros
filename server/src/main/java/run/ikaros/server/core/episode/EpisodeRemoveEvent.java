package run.ikaros.server.core.episode;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.EpisodeEntity;

@Getter
public class EpisodeRemoveEvent extends ApplicationEvent {
    private final EpisodeEntity entity;

    public EpisodeRemoveEvent(Object source, EpisodeEntity entity) {
        super(source);
        this.entity = entity;
    }

    public EpisodeRemoveEvent(Object source, Clock clock, EpisodeEntity entity) {
        super(source, clock);
        this.entity = entity;
    }
}
