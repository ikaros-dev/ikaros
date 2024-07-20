package run.ikaros.server.core.subject.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.SubjectEntity;

@Getter
public class SubjectUpdateEvent extends ApplicationEvent {
    private final SubjectEntity oldEntity;
    private final SubjectEntity newEntity;

    /**
     * Construct.
     */
    public SubjectUpdateEvent(Object source,
                              SubjectEntity oldEntity, SubjectEntity newEntity) {
        super(source);
        this.oldEntity = oldEntity;
        this.newEntity = newEntity;
    }
}
