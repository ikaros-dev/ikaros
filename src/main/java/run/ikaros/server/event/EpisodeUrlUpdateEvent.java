package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Getter
public class EpisodeUrlUpdateEvent extends ApplicationEvent {
    private final Long episodeId;
    private final String oldUrl;
    private final String newUrl;
    private final String newUrlFileName;
    private final Boolean isNotify;

    public EpisodeUrlUpdateEvent(@Nonnull Object source, @Nonnull Long episodeId,
                                 @Nullable String oldUrl, @Nonnull String newUrl,
                                 String newUrlFileName, @Nonnull Boolean isNotify) {
        super(source);
        this.episodeId = episodeId;
        this.oldUrl = oldUrl;
        this.newUrl = newUrl;
        this.newUrlFileName = newUrlFileName;
        this.isNotify = isNotify;
    }
}
