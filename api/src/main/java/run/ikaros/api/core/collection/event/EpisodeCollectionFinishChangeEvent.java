package run.ikaros.api.core.collection.event;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.plugin.event.PluginAwareEvent;

@Getter
public class EpisodeCollectionFinishChangeEvent extends PluginAwareEvent {
    private final UUID userId;
    private final UUID episodeId;
    private final boolean finish;
    @Setter
    private @Nullable UUID subjectId;

    /**
     * Construct.
     */
    public EpisodeCollectionFinishChangeEvent(Object source, UUID userId, UUID episodeId,
                                              boolean finish) {
        super(source);
        this.userId = userId;
        this.episodeId = episodeId;
        this.finish = finish;
    }

    /**
     * Construct.
     */
    public EpisodeCollectionFinishChangeEvent(Object source, String pluginId, UUID userId,
                                              UUID episodeId,
                                              boolean finish) {
        super(source, pluginId);
        this.userId = userId;
        this.episodeId = episodeId;
        this.finish = finish;
    }
}
