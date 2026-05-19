package run.ikaros.server.core.attachment.vo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BatchMatchingEpisodeAttachmentTest {

    @Test
    void defaultValues() {
        BatchMatchingEpisodeAttachment vo = new BatchMatchingEpisodeAttachment();
        assertNull(vo.getEpisodeId());
        assertNull(vo.getAttachmentIds());
    }

    @Test
    void settersAndGetters() {
        BatchMatchingEpisodeAttachment vo = new BatchMatchingEpisodeAttachment();
        UUID episodeId = UUID.randomUUID();
        UUID[] attachmentIds = {UUID.randomUUID(), UUID.randomUUID()};

        vo.setEpisodeId(episodeId);
        vo.setAttachmentIds(attachmentIds);

        assertEquals(episodeId, vo.getEpisodeId());
        assertArrayEquals(attachmentIds, vo.getAttachmentIds());
    }

    @Test
    void settersWithNull() {
        BatchMatchingEpisodeAttachment vo = new BatchMatchingEpisodeAttachment();
        vo.setEpisodeId(UUID.randomUUID());
        vo.setAttachmentIds(new UUID[]{UUID.randomUUID()});

        vo.setEpisodeId(null);
        vo.setAttachmentIds(null);

        assertNull(vo.getEpisodeId());
        assertNull(vo.getAttachmentIds());
    }
}
