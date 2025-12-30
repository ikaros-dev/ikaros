package run.ikaros.server.core.collection.event;

import java.time.Clock;
import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubjectCollectionCreateEvent extends ApplicationEvent {
    private final UUID subjectId;
    private final UUID userId;

    /**
     * Construct.
     */
    public SubjectCollectionCreateEvent(Object source, UUID subjectId, UUID userId) {
        super(source);
        this.subjectId = subjectId;
        this.userId = userId;
    }

    /**
     * Construct.
     */
    public SubjectCollectionCreateEvent(Object source, Clock clock, UUID subjectId, UUID userId) {
        super(source, clock);
        this.subjectId = subjectId;
        this.userId = userId;
    }
}
