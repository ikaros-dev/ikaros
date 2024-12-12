package run.ikaros.server.core.collection.event;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubjectCollectionCreateEvent extends ApplicationEvent {
    private final Long subjectId;
    private final Long userId;

    /**
     * Construct.
     */
    public SubjectCollectionCreateEvent(Object source, Long subjectId, Long userId) {
        super(source);
        this.subjectId = subjectId;
        this.userId = userId;
    }

    /**
     * Construct.
     */
    public SubjectCollectionCreateEvent(Object source, Clock clock, Long subjectId, Long userId) {
        super(source, clock);
        this.subjectId = subjectId;
        this.userId = userId;
    }
}
