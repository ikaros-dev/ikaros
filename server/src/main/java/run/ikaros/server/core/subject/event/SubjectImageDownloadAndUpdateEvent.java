package run.ikaros.server.core.subject.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.api.core.subject.SubjectImage;

@Getter
public class SubjectImageDownloadAndUpdateEvent extends ApplicationEvent {
    private final SubjectImage image;
    private final Long subjectId;

    /**
     * Construct.
     */
    public SubjectImageDownloadAndUpdateEvent(Object source, Long subjectId, SubjectImage image) {
        super(source);
        this.image = image;
        this.subjectId = subjectId;
    }
}
