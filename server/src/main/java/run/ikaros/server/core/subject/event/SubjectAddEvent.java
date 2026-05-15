package run.ikaros.server.core.subject.event;

import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.SubjectEntity;

public class SubjectAddEvent extends ApplicationEvent {
    private final SubjectEntity entity;

    public SubjectAddEvent(Object source, SubjectEntity entity) {
        super(source);
        this.entity = entity;
    }

    public SubjectEntity getEntity() {
        return entity;
    }
}
