package run.ikaros.api.core.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentType;

/** Attachment 数据模型测试. */
class AttachmentTest {

    @Test
    void builder_ShouldCreateAttachmentWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        LocalDateTime modifiedTime = LocalDateTime.now();

        Attachment attachment = Attachment.builder()
            .id(id)
            .parentId(parentId)
            .type(AttachmentType.File)
            .url("driver://path/to/file")
            .path("/path/to/file")
            .fsPath("/fs/path/to/file")
            .name("test.txt")
            .size(1024L)
            .updateTime(updateTime)
            .modifiedTime(modifiedTime)
            .deleted(false)
            .driverId(driverId)
            .sha1("abc123")
            .build();

        assertThat(attachment.getId()).isEqualTo(id);
        assertThat(attachment.getParentId()).isEqualTo(parentId);
        assertThat(attachment.getType()).isEqualTo(AttachmentType.File);
        assertThat(attachment.getUrl()).isEqualTo("driver://path/to/file");
        assertThat(attachment.getPath()).isEqualTo("/path/to/file");
        assertThat(attachment.getFsPath()).isEqualTo("/fs/path/to/file");
        assertThat(attachment.getName()).isEqualTo("test.txt");
        assertThat(attachment.getSize()).isEqualTo(1024L);
        assertThat(attachment.getUpdateTime()).isEqualTo(updateTime);
        assertThat(attachment.getModifiedTime()).isEqualTo(modifiedTime);
        assertThat(attachment.getDeleted()).isFalse();
        assertThat(attachment.getDriverId()).isEqualTo(driverId);
        assertThat(attachment.getSha1()).isEqualTo("abc123");
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyAttachment() {
        Attachment attachment = new Attachment();

        assertThat(attachment.getId()).isNull();
        assertThat(attachment.getParentId()).isNull();
        assertThat(attachment.getType()).isNull();
        assertThat(attachment.getUrl()).isNull();
        assertThat(attachment.getPath()).isNull();
        assertThat(attachment.getFsPath()).isNull();
        assertThat(attachment.getName()).isNull();
        assertThat(attachment.getSize()).isNull();
        assertThat(attachment.getUpdateTime()).isNull();
        assertThat(attachment.getModifiedTime()).isNull();
        assertThat(attachment.getDeleted()).isNull();
        assertThat(attachment.getDriverId()).isNull();
        assertThat(attachment.getSha1()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldCreateAttachmentWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        LocalDateTime modifiedTime = LocalDateTime.now();

        Attachment attachment = new Attachment(
            id, parentId, AttachmentType.File, "url", "path",
            "fsPath", "name", 1024L, updateTime, modifiedTime, false, driverId, "sha1"
        );

        assertThat(attachment.getId()).isEqualTo(id);
        assertThat(attachment.getParentId()).isEqualTo(parentId);
        assertThat(attachment.getType()).isEqualTo(AttachmentType.File);
        assertThat(attachment.getUrl()).isEqualTo("url");
        assertThat(attachment.getPath()).isEqualTo("path");
        assertThat(attachment.getFsPath()).isEqualTo("fsPath");
        assertThat(attachment.getName()).isEqualTo("name");
        assertThat(attachment.getSize()).isEqualTo(1024L);
        assertThat(attachment.getUpdateTime()).isEqualTo(updateTime);
        assertThat(attachment.getModifiedTime()).isEqualTo(modifiedTime);
        assertThat(attachment.getDeleted()).isFalse();
        assertThat(attachment.getDriverId()).isEqualTo(driverId);
        assertThat(attachment.getSha1()).isEqualTo("sha1");
    }

    @Test
    void setters_ShouldSetFields() {
        Attachment attachment = new Attachment();
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        LocalDateTime modifiedTime = LocalDateTime.now();

        attachment.setId(id);
        attachment.setParentId(parentId);
        attachment.setType(AttachmentType.Directory);
        attachment.setUrl("url");
        attachment.setPath("path");
        attachment.setFsPath("fsPath");
        attachment.setName("name");
        attachment.setSize(2048L);
        attachment.setUpdateTime(updateTime);
        attachment.setModifiedTime(modifiedTime);
        attachment.setDeleted(true);
        attachment.setDriverId(driverId);
        attachment.setSha1("sha1");

        assertThat(attachment.getId()).isEqualTo(id);
        assertThat(attachment.getParentId()).isEqualTo(parentId);
        assertThat(attachment.getType()).isEqualTo(AttachmentType.Directory);
        assertThat(attachment.getUrl()).isEqualTo("url");
        assertThat(attachment.getPath()).isEqualTo("path");
        assertThat(attachment.getFsPath()).isEqualTo("fsPath");
        assertThat(attachment.getName()).isEqualTo("name");
        assertThat(attachment.getSize()).isEqualTo(2048L);
        assertThat(attachment.getUpdateTime()).isEqualTo(updateTime);
        assertThat(attachment.getModifiedTime()).isEqualTo(modifiedTime);
        assertThat(attachment.getDeleted()).isTrue();
        assertThat(attachment.getDriverId()).isEqualTo(driverId);
        assertThat(attachment.getSha1()).isEqualTo("sha1");
    }

    @Test
    void chainAccessors_ShouldReturnThis() {
        Attachment attachment = new Attachment();

        Attachment result = attachment
            .setId(UUID.randomUUID())
            .setParentId(UUID.randomUUID())
            .setType(AttachmentType.File)
            .setUrl("url")
            .setPath("path")
            .setFsPath("fsPath")
            .setName("name")
            .setSize(1024L)
            .setUpdateTime(LocalDateTime.now())
            .setModifiedTime(LocalDateTime.now())
            .setDeleted(false)
            .setDriverId(UUID.randomUUID())
            .setSha1("sha1");

        assertThat(result).isSameAs(attachment);
    }

    @Test
    void equals_WithSameId_ShouldBeEqual() {
        UUID id = UUID.randomUUID();
        Attachment attachment1 = Attachment.builder().id(id).build();
        Attachment attachment2 = Attachment.builder().id(id).build();

        assertThat(attachment1).isEqualTo(attachment2);
    }

    @Test
    void hashCode_WithSameId_ShouldBeEqual() {
        UUID id = UUID.randomUUID();
        Attachment attachment1 = Attachment.builder().id(id).build();
        Attachment attachment2 = Attachment.builder().id(id).build();

        assertThat(attachment1).hasSameHashCodeAs(attachment2);
    }

    @Test
    void toString_ShouldContainAllFields() {
        Attachment attachment = Attachment.builder()
            .id(UUID.randomUUID())
            .name("test.txt")
            .type(AttachmentType.File)
            .build();

        String str = attachment.toString();
        assertThat(str).contains("test.txt", "File");
    }
}
