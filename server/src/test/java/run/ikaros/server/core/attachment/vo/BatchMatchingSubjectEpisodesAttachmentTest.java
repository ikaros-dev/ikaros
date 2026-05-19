package run.ikaros.server.core.attachment.vo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.EpisodeGroup;

class BatchMatchingSubjectEpisodesAttachmentTest {

    @Test
    void defaultValues() {
        BatchMatchingSubjectEpisodesAttachment vo = new BatchMatchingSubjectEpisodesAttachment();
        assertNull(vo.getSubjectId());
        assertNull(vo.getAttachmentIds());
        assertNull(vo.getEpisodeGroup());
    }

    @Test
    void settersAndGetters() {
        BatchMatchingSubjectEpisodesAttachment vo = new BatchMatchingSubjectEpisodesAttachment();
        UUID subjectId = UUID.randomUUID();
        UUID[] attachmentIds = {UUID.randomUUID(), UUID.randomUUID()};
        EpisodeGroup group = EpisodeGroup.MAIN;

        vo.setSubjectId(subjectId);
        vo.setAttachmentIds(attachmentIds);
        vo.setEpisodeGroup(group);

        assertEquals(subjectId, vo.getSubjectId());
        assertArrayEquals(attachmentIds, vo.getAttachmentIds());
        assertEquals(EpisodeGroup.MAIN, vo.getEpisodeGroup());
    }

    @Test
    void settersWithNull() {
        BatchMatchingSubjectEpisodesAttachment vo = new BatchMatchingSubjectEpisodesAttachment();
        vo.setSubjectId(UUID.randomUUID());
        vo.setAttachmentIds(new UUID[]{UUID.randomUUID()});
        vo.setEpisodeGroup(EpisodeGroup.MAIN);

        vo.setSubjectId(null);
        vo.setAttachmentIds(null);
        vo.setEpisodeGroup(null);

        assertNull(vo.getSubjectId());
        assertNull(vo.getAttachmentIds());
        assertNull(vo.getEpisodeGroup());
    }
}
