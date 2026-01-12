package run.ikaros.server.core.attachment.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.server.core.attachment.utils.AttachmentTestUtils.attIsEquals;
import static run.ikaros.server.core.attachment.utils.AttachmentTestUtils.entitiesIsEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.infra.utils.RandomUtils;
import run.ikaros.server.store.entity.AttachmentEntity;

public class AttachmentTestUtilsTest {
    @Test
    public void testEntitiesIsEquals() {
        final int size = RandomUtils.getRandom().nextInt(10, 100);
        List<AttachmentEntity> attachmentEntities = new ArrayList<>(size);
        List<Attachment> attachments = new ArrayList<>(size);

        final String namePrefix = RandomUtils.randomString(20);
        for (int i = 0; i < size; i++) {
            UUID uuid = UuidV7Utils.generateUuid();
            AttachmentEntity entity = AttachmentEntity.builder()
                .id(uuid)
                .parentId(ROOT_DIRECTORY_ID)
                .type(AttachmentType.File)
                .name(namePrefix + i)
                .size(Long.parseLong(String.valueOf(i)))
                .build();
            attachmentEntities.add(entity);

            Attachment att = Attachment.builder()
                .id(uuid)
                .parentId(ROOT_DIRECTORY_ID)
                .type(AttachmentType.File)
                .name(namePrefix + i)
                .size(Long.parseLong(String.valueOf(i)))
                .build();
            attachments.add(att);
        }

        assertThat(entitiesIsEquals(attachmentEntities, attachmentEntities)).isTrue();
        assertThat(attIsEquals(attachmentEntities, attachments)).isTrue();


    }
}
