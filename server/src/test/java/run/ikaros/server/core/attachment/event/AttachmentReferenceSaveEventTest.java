package run.ikaros.server.core.attachment.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

class AttachmentReferenceSaveEventTest {

    @Test
    void constructorAndGetters() {
        UUID attachmentId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();
        Object source = new Object();

        AttachmentReferenceSaveEvent event = new AttachmentReferenceSaveEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
        assertEquals(AttachmentReferenceType.SUBJECT, event.getEntity().getType());
        assertEquals(attachmentId, event.getEntity().getAttachmentId());
        assertEquals(referenceId, event.getEntity().getReferenceId());
    }

    @Test
    void isApplicationEvent() {
        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder().build();
        AttachmentReferenceSaveEvent event =
            new AttachmentReferenceSaveEvent(new Object(), entity);
        assertNotNull(event.getTimestamp());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        AttachmentReferenceEntity entity1 =
            AttachmentReferenceEntity.builder().type(AttachmentReferenceType.SUBJECT).build();
        AttachmentReferenceEntity entity2 =
            AttachmentReferenceEntity.builder().type(AttachmentReferenceType.EPISODE).build();

        AttachmentReferenceSaveEvent event1 = new AttachmentReferenceSaveEvent(source, entity1);
        AttachmentReferenceSaveEvent event2 = new AttachmentReferenceSaveEvent(source, entity2);

        assertEquals(entity1, event1.getEntity());
        assertEquals(entity2, event2.getEntity());
        assertSame(event1.getEntity(), entity1);
        assertSame(event2.getEntity(), entity2);
    }
}
