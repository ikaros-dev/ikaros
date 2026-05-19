package run.ikaros.server.core.attachment.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentDriverType;

class AttachmentDriverFetcherVoTest {

    @Test
    void defaultValues() {
        AttachmentDriverFetcherVo vo = new AttachmentDriverFetcherVo();
        assertNull(vo.getType());
        assertNull(vo.getName());
    }

    @Test
    void settersAndGetters() {
        AttachmentDriverFetcherVo vo = new AttachmentDriverFetcherVo();
        vo.setType(AttachmentDriverType.LOCAL);
        vo.setName("local-driver");

        assertEquals(AttachmentDriverType.LOCAL, vo.getType());
        assertEquals("local-driver", vo.getName());
    }

    @Test
    void settersWithNull() {
        AttachmentDriverFetcherVo vo = new AttachmentDriverFetcherVo();
        vo.setType(AttachmentDriverType.LOCAL);
        vo.setName("test");

        vo.setType(null);
        vo.setName(null);

        assertNull(vo.getType());
        assertNull(vo.getName());
    }
}
