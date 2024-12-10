package run.ikaros.server.core.collection.event;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubjectCollectionScoreUpdateEvent extends ApplicationEvent {
    private final Long subjectId;

    public SubjectCollectionScoreUpdateEvent(Object source, Long subjectId) {
        super(source);
        this.subjectId = subjectId;
    }

    public SubjectCollectionScoreUpdateEvent(Object source, Clock clock, Long subjectId) {
        super(source, clock);
        this.subjectId = subjectId;
    }
}
