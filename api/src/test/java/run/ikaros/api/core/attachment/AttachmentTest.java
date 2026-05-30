package run.ikaros.api.core.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentType;

class AttachmentTest {

    @Test
    void builder_ShouldCreateAttachmentWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        
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
            .deleted(false)
            .driverId(driverId)
            .sha1("abc123")
            .build();
        
        assertEquals(id, attachment.getId());
        assertEquals(parentId, attachment.getParentId());
        assertEquals(AttachmentType.File, attachment.getType());
        assertEquals("driver://path/to/file", attachment.getUrl());
        assertEquals("/path/to/file", attachment.getPath());
        assertEquals("/fs/path/to/file", attachment.getFsPath());
        assertEquals("test.txt", attachment.getName());
        assertEquals(1024L, attachment.getSize());
        assertEquals(updateTime, attachment.getUpdateTime());
        assertEquals(false, attachment.getDeleted());
        assertEquals(driverId, attachment.getDriverId());
        assertEquals("abc123", attachment.getSha1());
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyAttachment() {
        Attachment attachment = new Attachment();
        
        assertNull(attachment.getId());
        assertNull(attachment.getParentId());
        assertNull(attachment.getType());
        assertNull(attachment.getUrl());
        assertNull(attachment.getPath());
        assertNull(attachment.getFsPath());
        assertNull(attachment.getName());
        assertNull(attachment.getSize());
        assertNull(attachment.getUpdateTime());
        assertNull(attachment.getDeleted());
        assertNull(attachment.getDriverId());
        assertNull(attachment.getSha1());
    }

    @Test
    void allArgsConstructor_ShouldCreateAttachmentWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        
        Attachment attachment = new Attachment(
            id, parentId, AttachmentType.File, "url", "path", 
            "fsPath", "name", 1024L, updateTime, false, driverId, "sha1"
        );
        
        assertEquals(id, attachment.getId());
        assertEquals(parentId, attachment.getParentId());
        assertEquals(AttachmentType.File, attachment.getType());
        assertEquals("url", attachment.getUrl());
        assertEquals("path", attachment.getPath());
        assertEquals("fsPath", attachment.getFsPath());
        assertEquals("name", attachment.getName());
        assertEquals(1024L, attachment.getSize());
        assertEquals(updateTime, attachment.getUpdateTime());
        assertEquals(false, attachment.getDeleted());
        assertEquals(driverId, attachment.getDriverId());
        assertEquals("sha1", attachment.getSha1());
    }

    @Test
    void setters_ShouldSetFields() {
        Attachment attachment = new Attachment();
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime updateTime = LocalDateTime.now();
        
        attachment.setId(id);
        attachment.setParentId(parentId);
        attachment.setType(AttachmentType.Directory);
        attachment.setUrl("url");
        attachment.setPath("path");
        attachment.setFsPath("fsPath");
        attachment.setName("name");
        attachment.setSize(2048L);
        attachment.setUpdateTime(updateTime);
        attachment.setDeleted(true);
        attachment.setDriverId(driverId);
        attachment.setSha1("sha1");
        
        assertEquals(id, attachment.getId());
        assertEquals(parentId, attachment.getParentId());
        assertEquals(AttachmentType.Directory, attachment.getType());
        assertEquals("url", attachment.getUrl());
        assertEquals("path", attachment.getPath());
        assertEquals("fsPath", attachment.getFsPath());
        assertEquals("name", attachment.getName());
        assertEquals(2048L, attachment.getSize());
        assertEquals(updateTime, attachment.getUpdateTime());
        assertEquals(true, attachment.getDeleted());
        assertEquals(driverId, attachment.getDriverId());
        assertEquals("sha1", attachment.getSha1());
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
            .setDeleted(false)
            .setDriverId(UUID.randomUUID())
            .setSha1("sha1");
        
        assertSame(attachment, result);
    }

    @Test
    void equals_WithSameId_ShouldBeEqual() {
        UUID id = UUID.randomUUID();
        Attachment attachment1 = Attachment.builder().id(id).build();
        Attachment attachment2 = Attachment.builder().id(id).build();
        
        assertEquals(attachment1, attachment2);
    }

    @Test
    void hashCode_WithSameId_ShouldBeEqual() {
        UUID id = UUID.randomUUID();
        Attachment attachment1 = Attachment.builder().id(id).build();
        Attachment attachment2 = Attachment.builder().id(id).build();
        
        assertEquals(attachment1.hashCode(), attachment2.hashCode());
    }

    @Test
    void toString_ShouldContainAllFields() {
        Attachment attachment = Attachment.builder()
            .id(UUID.randomUUID())
            .name("test.txt")
            .type(AttachmentType.File)
            .build();
        
        String str = attachment.toString();
        assertTrue(str.contains("test.txt"));
        assertTrue(str.contains("File"));
    }
}