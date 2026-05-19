package run.ikaros.server.core.subject.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.SubjectEntity;

class SubjectRemoveEventTest {

    @Test
    void constructorAndGetters() {
        UUID subjectId = UUID.randomUUID();
        SubjectEntity entity = SubjectEntity.builder()
            .name("Test Subject")
            .build();
        entity.setId(subjectId);
        Object source = new Object();

        SubjectRemoveEvent event = new SubjectRemoveEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
        assertEquals(subjectId, event.getEntity().getId());
        assertEquals("Test Subject", event.getEntity().getName());
    }

    @Test
    void isApplicationEvent() {
        SubjectRemoveEvent event = new SubjectRemoveEvent(new Object(),
            SubjectEntity.builder().build());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullEntity() {
        Object source = new Object();
        SubjectRemoveEvent event = new SubjectRemoveEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        SubjectEntity entity1 = SubjectEntity.builder().name("Subject 1").build();
        SubjectEntity entity2 = SubjectEntity.builder().name("Subject 2").build();

        SubjectRemoveEvent event1 = new SubjectRemoveEvent(source, entity1);
        SubjectRemoveEvent event2 = new SubjectRemoveEvent(source, entity2);

        assertEquals(entity1, event1.getEntity());
        assertEquals(entity2, event2.getEntity());
        assertSame(event1.getEntity(), entity1);
        assertSame(event2.getEntity(), entity2);
    }
}
