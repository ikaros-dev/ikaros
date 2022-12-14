package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.entity.EpisodeEntity;

@Getter
public class EpisodeUrlUpdateEvent extends ApplicationEvent {
    private final EpisodeEntity episodeEntity;

    public EpisodeUrlUpdateEvent(Object source, EpisodeEntity episodeEntity) {
        super(source);
        this.episodeEntity = episodeEntity;
    }
}
