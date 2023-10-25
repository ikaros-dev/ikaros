package run.ikaros.server.core.attachment.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.AttachmentEntity;

@Getter
public class AttachmentRemoveEvent extends ApplicationEvent {
    private final AttachmentEntity entity;

    public AttachmentRemoveEvent(Object source, AttachmentEntity entity) {
        super(source);
        this.entity = entity;
    }
}
