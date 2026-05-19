package run.ikaros.server.core.attachment.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EpisodeAttachmentUpdateEventTest {

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        UUID episodeId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Boolean notify = true;

        EpisodeAttachmentUpdateEvent event =
            new EpisodeAttachmentUpdateEvent(source, episodeId, attachmentId, notify);

        assertSame(source, event.getSource());
        assertEquals(episodeId, event.getEpisodeId());
        assertEquals(attachmentId, event.getAttachmentId());
        assertEquals(notify, event.getNotify());
    }

    @Test
    void isApplicationEvent() {
        EpisodeAttachmentUpdateEvent event = new EpisodeAttachmentUpdateEvent(
            new Object(), UUID.randomUUID(), UUID.randomUUID(), false);
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullFields() {
        Object source = new Object();
        EpisodeAttachmentUpdateEvent event =
            new EpisodeAttachmentUpdateEvent(source, null, null, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getEpisodeId());
        assertEquals(null, event.getAttachmentId());
        assertEquals(null, event.getNotify());
    }

    @Test
    void notifyFalse() {
        Object source = new Object();
        UUID episodeId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        EpisodeAttachmentUpdateEvent event =
            new EpisodeAttachmentUpdateEvent(source, episodeId, attachmentId, false);

        assertEquals(false, event.getNotify());
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        UUID episodeId1 = UUID.randomUUID();
        UUID attachmentId1 = UUID.randomUUID();
        UUID episodeId2 = UUID.randomUUID();
        UUID attachmentId2 = UUID.randomUUID();

        EpisodeAttachmentUpdateEvent event1 =
            new EpisodeAttachmentUpdateEvent(source, episodeId1, attachmentId1, true);
        EpisodeAttachmentUpdateEvent event2 =
            new EpisodeAttachmentUpdateEvent(source, episodeId2, attachmentId2, false);

        assertEquals(episodeId1, event1.getEpisodeId());
        assertEquals(attachmentId1, event1.getAttachmentId());
        assertEquals(true, event1.getNotify());
        assertEquals(episodeId2, event2.getEpisodeId());
        assertEquals(attachmentId2, event2.getAttachmentId());
        assertEquals(false, event2.getNotify());
    }
}
