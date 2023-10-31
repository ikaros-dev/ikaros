package run.ikaros.api.core.collection.event;

import lombok.Getter;
import run.ikaros.api.plugin.event.PluginAwareEvent;

@Getter
public class SubjectCollectProgressChangeEvent extends PluginAwareEvent {
    private final long userId;
    private final long subjectId;
    private final int oldProgress;
    private final int progress;

    /**
     * Construct.
     */
    public SubjectCollectProgressChangeEvent(Object source, long userId, long subjectId,
                                             int oldProgress, int progress) {
        super(source);
        this.userId = userId;
        this.subjectId = subjectId;
        this.oldProgress = oldProgress;
        this.progress = progress;
    }
}
