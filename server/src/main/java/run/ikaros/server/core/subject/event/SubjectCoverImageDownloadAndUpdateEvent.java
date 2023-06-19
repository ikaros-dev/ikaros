package run.ikaros.server.core.subject.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubjectCoverImageDownloadAndUpdateEvent extends ApplicationEvent {
    private final Long subjectId;
    private final String coverUrl;

    /**
     * Construct.
     */
    public SubjectCoverImageDownloadAndUpdateEvent(Object source, Long subjectId, String coverUrl) {
        super(source);
        this.subjectId = subjectId;
        this.coverUrl = coverUrl;
    }
}
