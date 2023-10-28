package run.ikaros.api.core.collection.event;

import lombok.Getter;
import run.ikaros.api.core.collection.SubjectCollection;

@Getter
public class SubjectUnCollectEvent extends SubjectCollectionUpdateEvent {
    /**
     * Construct.
     */
    public SubjectUnCollectEvent(Object source,
                                 SubjectCollection subjectCollection) {
        super(source, subjectCollection, false);
    }

    /**
     * Construct.
     */
    public SubjectUnCollectEvent(Object source, String pluginId,
                                 SubjectCollection subjectCollection,
                                 boolean collect) {
        super(source, pluginId, subjectCollection, collect);
    }
}
