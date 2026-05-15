package run.ikaros.server.core.attachment.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

@Getter
public class AttachmentDriverEnableEvent extends ApplicationEvent {
    private final AttachmentDriverEntity entity;

    public AttachmentDriverEnableEvent(Object source, AttachmentDriverEntity entity) {
        super(source);
        this.entity = entity;
    }
}
