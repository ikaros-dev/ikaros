package run.ikaros.server.core.attachment.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentType;

/** 本地磁盘附件驱动增量扫描测试. */
class LocalDiskAttachmentDriverFetcherTest {
    @Test
    void getChildrenOnlyReadsMetadataAndCalculatesSha1OnDemand(@TempDir Path tempDir)
        throws IOException {
        UUID driverId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Path file = tempDir.resolve("episode.mkv");
        Files.writeString(file, "episode-content");
        LocalAttachmentPathValidator pathValidator = new LocalAttachmentPathValidator();
        pathValidator.register(driverId, tempDir.toString());
        LocalDiskAttachmentDriverFetcher fetcher =
            new LocalDiskAttachmentDriverFetcher(pathValidator);

        StepVerifier.create(fetcher.getChildren(driverId, parentId, tempDir.toString()))
            .assertNext(attachment -> {
                assertThat(attachment.getType()).isEqualTo(AttachmentType.Driver_File);
                assertThat(attachment.getModifiedTime()).isNotNull();
                assertThat(attachment.getSha1()).isNull();

                StepVerifier.create(fetcher.calculateSha1(attachment))
                    .assertNext(hashedAttachment ->
                        assertThat(hashedAttachment.getSha1()).isNotBlank())
                    .verifyComplete();
            })
            .verifyComplete();
    }
}
