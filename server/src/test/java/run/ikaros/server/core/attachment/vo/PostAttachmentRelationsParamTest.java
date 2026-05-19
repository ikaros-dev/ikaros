package run.ikaros.server.core.attachment.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentRelationType;

class PostAttachmentRelationsParamTest {

    @Test
    void defaultValues() {
        PostAttachmentRelationsParam param = new PostAttachmentRelationsParam();
        assertNull(param.getMasterId());
        assertNull(param.getType());
        assertNull(param.getRelationIds());
    }

    @Test
    void settersAndGetters() {
        PostAttachmentRelationsParam param = new PostAttachmentRelationsParam();
        UUID masterId = UUID.randomUUID();
        UUID relId1 = UUID.randomUUID();
        UUID relId2 = UUID.randomUUID();
        List<UUID> relationIds = List.of(relId1, relId2);

        param.setMasterId(masterId);
        param.setType(AttachmentRelationType.VIDEO_SUBTITLE);
        param.setRelationIds(relationIds);

        assertEquals(masterId, param.getMasterId());
        assertEquals(AttachmentRelationType.VIDEO_SUBTITLE, param.getType());
        assertEquals(2, param.getRelationIds().size());
        assertEquals(relId1, param.getRelationIds().get(0));
        assertEquals(relId2, param.getRelationIds().get(1));
    }

    @Test
    void settersWithNull() {
        PostAttachmentRelationsParam param = new PostAttachmentRelationsParam();
        param.setMasterId(UUID.randomUUID());
        param.setType(AttachmentRelationType.VIDEO_SUBTITLE);
        param.setRelationIds(List.of(UUID.randomUUID()));

        param.setMasterId(null);
        param.setType(null);
        param.setRelationIds(null);

        assertNull(param.getMasterId());
        assertNull(param.getType());
        assertNull(param.getRelationIds());
    }

    @Test
    void settersWithEmptyList() {
        PostAttachmentRelationsParam param = new PostAttachmentRelationsParam();
        param.setRelationIds(List.of());
        assertEquals(0, param.getRelationIds().size());
    }
}
