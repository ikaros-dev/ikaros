package run.ikaros.server.core.attachment.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;

class AttachmentRemoveEventTest {

    private AttachmentEntity buildAttachmentEntity() {
        return AttachmentEntity.builder()
            .id(UUID.randomUUID())
            .type(AttachmentType.File)
            .name("test-file.txt")
            .url("https://example.com/test-file.txt")
            .path("/test/test-file.txt")
            .size(1024L)
            .deleted(false)
            .build();
    }

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        AttachmentEntity entity = buildAttachmentEntity();

        AttachmentRemoveEvent event = new AttachmentRemoveEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void isApplicationEvent() {
        AttachmentRemoveEvent event = new AttachmentRemoveEvent(new Object(), buildAttachmentEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullEntity() {
        Object source = new Object();
        AttachmentRemoveEvent event = new AttachmentRemoveEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        AttachmentEntity entity1 = buildAttachmentEntity();
        AttachmentEntity entity2 = buildAttachmentEntity();

        AttachmentRemoveEvent event1 = new AttachmentRemoveEvent(source, entity1);
        AttachmentRemoveEvent event2 = new AttachmentRemoveEvent(source, entity2);

        assertEquals(entity1, event1.getEntity());
        assertEquals(entity2, event2.getEntity());
        assertSame(event1.getEntity(), entity1);
        assertSame(event2.getEntity(), entity2);
    }
}
