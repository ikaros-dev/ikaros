package run.ikaros.server.core.tag.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.TagEntity;

class TagEventTest {

    private TagEntity buildTagEntity() {
        return TagEntity.builder()
            .id(UUID.randomUUID())
            .type(TagType.SUBJECT)
            .name("testTag")
            .masterId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .createTime(LocalDateTime.now())
            .color("#FF0000")
            .build();
    }

    @Test
    void tagCreateEventGetters() {
        Object source = new Object();
        TagEntity entity = buildTagEntity();
        TagCreateEvent event = new TagCreateEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void tagCreateEventWithClock() {
        Object source = new Object();
        TagEntity entity = buildTagEntity();
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        TagCreateEvent event = new TagCreateEvent(source, clock, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void tagRemoveEventGetters() {
        Object source = new Object();
        TagEntity entity = buildTagEntity();
        TagRemoveEvent event = new TagRemoveEvent(source, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void tagRemoveEventWithClock() {
        Object source = new Object();
        TagEntity entity = buildTagEntity();
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        TagRemoveEvent event = new TagRemoveEvent(source, clock, entity);

        assertSame(source, event.getSource());
        assertNotNull(event.getEntity());
        assertEquals(entity, event.getEntity());
    }

    @Test
    void tagCreateEventIsApplicationEvent() {
        TagCreateEvent event = new TagCreateEvent(new Object(), buildTagEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void tagRemoveEventIsApplicationEvent() {
        TagRemoveEvent event = new TagRemoveEvent(new Object(), buildTagEntity());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void tagCreateEventWithNullEntity() {
        Object source = new Object();
        TagCreateEvent event = new TagCreateEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEntity());
    }

    @Test
    void tagRemoveEventWithNullEntity() {
        Object source = new Object();
        TagRemoveEvent event = new TagRemoveEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEntity());
    }
}
