package run.ikaros.api.core.collection.event;

import lombok.Getter;
import run.ikaros.api.core.collection.SubjectCollection;

@Getter
public class SubjectCollectEvent extends SubjectCollectionUpdateEvent {
    /**
     * Construct.
     */
    public SubjectCollectEvent(Object source,
                               SubjectCollection subjectCollection) {
        super(source, subjectCollection, true);
    }

    /**
     * Construct.
     */
    public SubjectCollectEvent(Object source, String pluginId,
                               SubjectCollection subjectCollection, boolean collect) {
        super(source, pluginId, subjectCollection, collect);
    }
}
