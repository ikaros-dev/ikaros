package run.ikaros.server.core.attachment.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

class AttachmentDriverDisableEventTest {

    @Test
    void constructorAndGetters() {
        UUID driverId = UUID.randomUUID();
        AttachmentDriverEntity entity = AttachmentDriverEntity.builder()
            .id(driverId)
            .type(AttachmentDriverType.LOCAL)
            .name("local-driver")
            .enable(true)
            .build();
        Object source = new Object();

        AttachmentDriverDisableEvent event = new AttachmentDriverDisableEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
        assertEquals(driverId, event.getEntity().getId());
        assertEquals(AttachmentDriverType.LOCAL, event.getEntity().getType());
        assertEquals("local-driver", event.getEntity().getName());
    }

    @Test
    void isApplicationEvent() {
        AttachmentDriverEntity entity = AttachmentDriverEntity.builder().build();
        AttachmentDriverDisableEvent event = new AttachmentDriverDisableEvent(new Object(), entity);
        assertNotNull(event.getTimestamp());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        AttachmentDriverEntity entity1 = AttachmentDriverEntity.builder().name("driver-1").build();
        AttachmentDriverEntity entity2 = AttachmentDriverEntity.builder().name("driver-2").build();

        AttachmentDriverDisableEvent event1 = new AttachmentDriverDisableEvent(source, entity1);
        AttachmentDriverDisableEvent event2 = new AttachmentDriverDisableEvent(source, entity2);

        assertEquals(entity1, event1.getEntity());
        assertEquals(entity2, event2.getEntity());
        assertSame(event1.getEntity(), entity1);
        assertSame(event2.getEntity(), entity2);
    }
}
