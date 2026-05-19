package run.ikaros.server.core.subject.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.entity.SubjectEntity;

class SubjectUpdateEventTest {

    private SubjectEntity buildSubjectEntity(String name) {
        SubjectEntity entity = SubjectEntity.builder()
            .type(SubjectType.ANIME)
            .name(name)
            .nameCn(name + " Cn")
            .nsfw(false)
            .cover("https://example.com/cover.png")
            .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        SubjectEntity oldEntity = buildSubjectEntity("Old Name");
        SubjectEntity newEntity = buildSubjectEntity("New Name");

        SubjectUpdateEvent event = new SubjectUpdateEvent(source, oldEntity, newEntity);

        assertSame(source, event.getSource());
        assertNotNull(event.getOldEntity());
        assertNotNull(event.getNewEntity());
        assertEquals(oldEntity, event.getOldEntity());
        assertEquals(newEntity, event.getNewEntity());
    }

    @Test
    void isApplicationEvent() {
        SubjectUpdateEvent event = new SubjectUpdateEvent(
            new Object(), buildSubjectEntity("old"), buildSubjectEntity("new"));
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullEntities() {
        Object source = new Object();
        SubjectUpdateEvent event = new SubjectUpdateEvent(source, null, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getOldEntity());
        assertEquals(null, event.getNewEntity());
    }

    @Test
    void oldAndNewEntitiesAreDistinct() {
        Object source = new Object();
        SubjectEntity oldEntity = buildSubjectEntity("Old");
        SubjectEntity newEntity = buildSubjectEntity("New");

        SubjectUpdateEvent event = new SubjectUpdateEvent(source, oldEntity, newEntity);

        assertSame(oldEntity, event.getOldEntity());
        assertSame(newEntity, event.getNewEntity());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        SubjectEntity oldEntity1 = buildSubjectEntity("Old1");
        SubjectEntity newEntity1 = buildSubjectEntity("New1");
        SubjectEntity oldEntity2 = buildSubjectEntity("Old2");
        SubjectEntity newEntity2 = buildSubjectEntity("New2");

        SubjectUpdateEvent event1 = new SubjectUpdateEvent(source, oldEntity1, newEntity1);
        SubjectUpdateEvent event2 = new SubjectUpdateEvent(source, oldEntity2, newEntity2);

        assertEquals(oldEntity1, event1.getOldEntity());
        assertEquals(newEntity1, event1.getNewEntity());
        assertEquals(oldEntity2, event2.getOldEntity());
        assertEquals(newEntity2, event2.getNewEntity());
    }
}
