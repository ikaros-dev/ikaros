package run.ikaros.server.core.attachment.extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;

@Slf4j
@Extension
@Component
public class LocalDiskAttachmentDriverFetcher implements AttachmentDriverFetcher {
    public static String LOCAL_DISK_DRIVER_NAME = "DISK";

    private AttachmentDriver driver;


    @Override
    public AttachmentDriverType getDriverType() {
        return AttachmentDriverType.LOCAL;
    }

    @Override
    public String getDriverName() {
        return LOCAL_DISK_DRIVER_NAME;
    }

    @Override
    public void setDriver(AttachmentDriver driver) {
        this.driver = driver;
    }

    @Override
    public List<Attachment> getChildAttachments(Long pid, String remotePath) {
        if (driver == null || driver.getId() == null) {
            throw new UnsupportedOperationException("Please set driver first.");
        }
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
                    sha1 = FileUtils.calculateSha1(f.getAbsolutePath());
                }
            } catch (IOException ioException) {
                log.warn("File size error: {}", ioException.getMessage());
            } catch (NoSuchAlgorithmException e) {
                log.warn("File sha1 error: {}", e.getMessage());
            }

            Attachment attachment = Attachment.builder()
                .parentId(pid)
                .type(f.isFile() ? AttachmentType.Driver_File : AttachmentType.Driver_Directory)
                .name(f.getName())
                .path(f.getPath())
                .fsPath(f.getAbsolutePath())
                .size(size)
                .sha1(sha1)
                .updateTime(LocalDateTime.now())
                .deleted(false)
                .driverId(driver.getId())
                .build();
            attachments.add(attachment);
        }

        return attachments;
    }

}
