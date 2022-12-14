package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.entity.EpisodeEntity;

@Getter
public class EpisodeUrlUpdateEvent extends ApplicationEvent {
    private final EpisodeEntity episodeEntity;
    private final String oldUrl;

    public EpisodeUrlUpdateEvent(Object source, EpisodeEntity episodeEntity, String oldUrl) {
        super(source);
        this.episodeEntity = episodeEntity;
        this.oldUrl = oldUrl;
    }
}
