package run.ikaros.server.core.attachment.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

@Getter
public class AttachmentReferenceSaveEvent extends ApplicationEvent {

    private final AttachmentReferenceEntity entity;

    public AttachmentReferenceSaveEvent(Object source, AttachmentReferenceEntity entity) {
        super(source);
        this.entity = entity;
    }

}
