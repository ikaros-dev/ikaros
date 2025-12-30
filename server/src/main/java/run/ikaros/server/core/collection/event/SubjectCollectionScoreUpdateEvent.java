package run.ikaros.server.core.collection.event;

import java.time.Clock;
import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubjectCollectionScoreUpdateEvent extends ApplicationEvent {
    private final UUID subjectId;

    public SubjectCollectionScoreUpdateEvent(Object source, UUID subjectId) {
        super(source);
        this.subjectId = subjectId;
    }

    public SubjectCollectionScoreUpdateEvent(Object source, Clock clock, UUID subjectId) {
        super(source, clock);
        this.subjectId = subjectId;
    }
}
