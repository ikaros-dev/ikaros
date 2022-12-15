package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public class EpisodeUrlUpdateEvent extends ApplicationEvent {
    private final Long episodeId;
    private final String oldUrl;
    private final String newUrl;
    private final Boolean isNotify;

    public EpisodeUrlUpdateEvent(@Nonnull Object source, @Nonnull Long episodeId,
                                 @Nullable String oldUrl, @Nonnull String newUrl,
                                 @Nonnull Boolean isNotify) {
        super(source);
        this.episodeId = episodeId;
        this.oldUrl = oldUrl;
        this.newUrl = newUrl;
        this.isNotify = isNotify;
    }
}
