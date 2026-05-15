package run.ikaros.api.core.collection.event;

import lombok.Getter;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.plugin.event.PluginAwareEvent;

@Getter
public class SubjectCollectionUpdateEvent extends PluginAwareEvent {
    private final SubjectCollection subjectCollection;
    /**
     * <ul>
     *     <li>true - collect action</li>
     *     <li>false - uncollect action</li>
     * </ul>
     * .
     */
    private final boolean collect;

    /**
     * Construct.
     */
    public SubjectCollectionUpdateEvent(Object source, SubjectCollection subjectCollection,
                                        boolean collect) {
        super(source);
        this.subjectCollection = subjectCollection;
        this.collect = collect;
    }

    /**
     * Construct.
     */
    public SubjectCollectionUpdateEvent(Object source, String pluginId,
                                        SubjectCollection subjectCollection, boolean collect) {
        super(source, pluginId);
        this.subjectCollection = subjectCollection;
        this.collect = collect;
    }
}
