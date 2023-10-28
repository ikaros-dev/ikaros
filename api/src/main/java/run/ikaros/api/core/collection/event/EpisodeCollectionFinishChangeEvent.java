package run.ikaros.api.core.collection.event;

import lombok.Getter;
import lombok.Setter;
import run.ikaros.api.plugin.event.PluginAwareEvent;

@Getter
public class EpisodeCollectionFinishChangeEvent extends PluginAwareEvent {
    private final long userId;
    private final long episodeId;
    private final boolean finish;
    @Setter
    private long subjectId;

    /**
     * Construct.
     */
    public EpisodeCollectionFinishChangeEvent(Object source, long userId, long episodeId,
                                              boolean finish) {
        super(source);
        this.userId = userId;
        this.episodeId = episodeId;
        this.finish = finish;
    }

    /**
     * Construct.
     */
    public EpisodeCollectionFinishChangeEvent(Object source, String pluginId, long userId,
                                              long episodeId,
                                              boolean finish) {
        super(source, pluginId);
        this.userId = userId;
        this.episodeId = episodeId;
        this.finish = finish;
    }
}
