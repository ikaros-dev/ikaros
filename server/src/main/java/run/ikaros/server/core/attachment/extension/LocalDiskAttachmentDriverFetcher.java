package run.ikaros.server.core.attachment.extension;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;

@Slf4j
@Extension
@Component
public class LocalDiskAttachmentDriverFetcher implements AttachmentDriverFetcher {
    public static String LOCAL_DISK_DRIVER_NAME = "DISK";


    @Override
    public AttachmentDriverType getDriverType() {
        return AttachmentDriverType.LOCAL;
    }

    @Override
    public String getDriverName() {
        return LOCAL_DISK_DRIVER_NAME;
    }

    @Override
    public List<Attachment> getChildren(Long driverId, Long parentAttId, String remotePath) {
        Assert.isTrue(driverId >= 0, "driverId must be greater than or equal to zero.");
        Assert.isTrue(parentAttId >= 0, "driverId must be greater than or equal to zero.");
        Assert.hasText(remotePath, "remotePath must not be empty.");
        File file = new File(remotePath);
        Path path = Paths.get(remotePath);
        File[] files = path.toFile().listFiles();
        if (files == null) {
            return List.of();
        }
        List<Attachment> attachments = new ArrayList<>();
        for (File f : files) {
            long size = 0;
            String sha1 = "";
            try {
                size = Files.size(Path.of(f.toURI()));
                if (f.isFile()) {
                    // sha1 = FileUtils.calculateSha1(f.getAbsolutePath());
                    sha1 = "";
                }
            } catch (IOException ioException) {
                log.warn("File size error: {}", ioException.getMessage());
            }

            Attachment attachment = Attachment.builder()
                .parentId(parentAttId)
                .type(f.isFile() ? AttachmentType.Driver_File : AttachmentType.Driver_Directory)
                .name(f.getName())
                .path(f.getPath())
                .fsPath(f.getAbsolutePath())
                .size(size)
                .sha1(sha1)
                .updateTime(LocalDateTime.now())
                .deleted(false)
                .driverId(driverId)
                .build();
            attachments.add(attachment);
        }

        return attachments;
    }

    @Override
    public String parseReadUrl(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        return DRIVER_STATIC_RESOURCE_PREFIX + attachment.getPath();
    }

    @Override
    public String parseDownloadUrl(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        return DRIVER_STATIC_RESOURCE_PREFIX + attachment.getPath();
    }


}
