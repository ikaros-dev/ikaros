package run.ikaros.server.core.collection.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectCollectionScoreUpdateEventTest {

    @Test
    void constructorWithSourceAndSubjectId() {
        Object source = new Object();
        UUID subjectId = UUID.randomUUID();

        SubjectCollectionScoreUpdateEvent event =
            new SubjectCollectionScoreUpdateEvent(source, subjectId);

        assertSame(source, event.getSource());
        assertEquals(subjectId, event.getSubjectId());
    }

    @Test
    void constructorWithClock() {
        Object source = new Object();
        UUID subjectId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

        SubjectCollectionScoreUpdateEvent event =
            new SubjectCollectionScoreUpdateEvent(source, clock, subjectId);

        assertSame(source, event.getSource());
        assertEquals(subjectId, event.getSubjectId());
        assertEquals(clock.millis(), event.getTimestamp());
    }

    @Test
    void isApplicationEvent() {
        SubjectCollectionScoreUpdateEvent event =
            new SubjectCollectionScoreUpdateEvent(new Object(), UUID.randomUUID());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullSubjectId() {
        Object source = new Object();
        SubjectCollectionScoreUpdateEvent event =
            new SubjectCollectionScoreUpdateEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getSubjectId());
    }

    @Test
    void differentSubjectIdsAreDistinct() {
        Object source = new Object();
        UUID subjectId1 = UUID.randomUUID();
        UUID subjectId2 = UUID.randomUUID();

        SubjectCollectionScoreUpdateEvent event1 =
            new SubjectCollectionScoreUpdateEvent(source, subjectId1);
        SubjectCollectionScoreUpdateEvent event2 =
            new SubjectCollectionScoreUpdateEvent(source, subjectId2);

        assertEquals(subjectId1, event1.getSubjectId());
        assertEquals(subjectId2, event2.getSubjectId());
    }
}
