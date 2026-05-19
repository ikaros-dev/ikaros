package run.ikaros.server.core.collection.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectCollectionCreateEventTest {

    @Test
    void constructorWithSourceSubjectIdAndUserId() {
        Object source = new Object();
        UUID subjectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SubjectCollectionCreateEvent event =
            new SubjectCollectionCreateEvent(source, subjectId, userId);

        assertSame(source, event.getSource());
        assertEquals(subjectId, event.getSubjectId());
        assertEquals(userId, event.getUserId());
    }

    @Test
    void constructorWithClock() {
        Object source = new Object();
        UUID subjectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

        SubjectCollectionCreateEvent event =
            new SubjectCollectionCreateEvent(source, clock, subjectId, userId);

        assertSame(source, event.getSource());
        assertEquals(subjectId, event.getSubjectId());
        assertEquals(userId, event.getUserId());
        assertEquals(clock.millis(), event.getTimestamp());
    }

    @Test
    void isApplicationEvent() {
        SubjectCollectionCreateEvent event =
            new SubjectCollectionCreateEvent(new Object(), UUID.randomUUID(), UUID.randomUUID());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullIds() {
        Object source = new Object();
        SubjectCollectionCreateEvent event =
            new SubjectCollectionCreateEvent(source, null, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getSubjectId());
        assertEquals(null, event.getUserId());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        UUID subjectId1 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID subjectId2 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        SubjectCollectionCreateEvent event1 =
            new SubjectCollectionCreateEvent(source, subjectId1, userId1);
        SubjectCollectionCreateEvent event2 =
            new SubjectCollectionCreateEvent(source, subjectId2, userId2);

        assertEquals(subjectId1, event1.getSubjectId());
        assertEquals(userId1, event1.getUserId());
        assertEquals(subjectId2, event2.getSubjectId());
        assertEquals(userId2, event2.getUserId());
    }
}
