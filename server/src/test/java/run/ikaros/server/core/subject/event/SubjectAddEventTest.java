package run.ikaros.server.core.subject.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.entity.SubjectEntity;

class SubjectAddEventTest {

    private SubjectEntity buildSubjectEntity() {
        SubjectEntity entity = SubjectEntity.builder()
            .type(SubjectType.ANIME)
            .name("Test Subject")
            .nameCn("测试主体")
            .nsfw(false)
            .cover("https://example.com/cover.png")
            .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        SubjectEntity entity = buildSubjectEntity();

        SubjectAddEvent event = new SubjectAddEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void isApplicationEvent() {
        SubjectAddEvent event = new SubjectAddEvent(new Object(), buildSubjectEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullEntity() {
        Object source = new Object();
        SubjectAddEvent event = new SubjectAddEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        SubjectEntity entity1 = buildSubjectEntity();
        SubjectEntity entity2 = buildSubjectEntity();

        SubjectAddEvent event1 = new SubjectAddEvent(source, entity1);
        SubjectAddEvent event2 = new SubjectAddEvent(source, entity2);

        assertEquals(entity1, event1.getEntity());
        assertEquals(entity2, event2.getEntity());
        assertSame(event1.getEntity(), entity1);
        assertSame(event2.getEntity(), entity2);
    }
}
