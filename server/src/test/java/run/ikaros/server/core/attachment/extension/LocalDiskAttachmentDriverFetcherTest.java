package run.ikaros.server.core.attachment.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentType;

/** 本地磁盘附件驱动测试. */
class LocalDiskAttachmentDriverFetcherTest {
    @Test
    void getChildrenReturnsFileWithSha1ImmediatelyCalculated(@TempDir Path tempDir)
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
                // 当前实现立即计算sha1，且不记录modifiedTime
                assertThat(attachment.getSha1()).isNotBlank();
                assertThat(attachment.getModifiedTime()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void getChildrenReturnsFilesAndDirectoriesWithCurrentParent(@TempDir Path tempDir)
        throws IOException {
        UUID driverId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Files.writeString(tempDir.resolve("episode.mkv"), "episode-content");
        Files.createDirectory(tempDir.resolve("season-2"));
        LocalDiskAttachmentDriverFetcher fetcher = createFetcher(driverId, tempDir);

        List<Attachment> children = fetcher.getChildren(driverId, parentId, tempDir.toString())
            .collectList()
            .block();

        assertThat(children).isNotNull().hasSize(2);
        assertThat(children).allSatisfy(attachment -> {
            assertThat(attachment.getParentId()).isEqualTo(parentId);
            assertThat(attachment.getDriverId()).isEqualTo(driverId);
            assertThat(attachment.getFsPath()).isNotBlank();
            // 当前实现不记录modifiedTime
            assertThat(attachment.getModifiedTime()).isNull();
        });
        assertThat(children).extracting(Attachment::getType)
            .containsExactlyInAnyOrder(
                AttachmentType.Driver_File, AttachmentType.Driver_Directory);
    }

    @Test
    void getChildrenRejectsReadableFileAsDirectory(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path file = Files.writeString(tempDir.resolve("episode.mkv"), "episode-content");
        LocalDiskAttachmentDriverFetcher fetcher = createFetcher(driverId, tempDir);

        StepVerifier.create(fetcher.getChildren(driverId, UUID.randomUUID(), file.toString()))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标路径不是可读取目录"))
            .verify();
    }

    @Test
    void calculateSha1ReturnsAttachmentUnchanged(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        LocalDiskAttachmentDriverFetcher fetcher = createFetcher(driverId, tempDir);
        Attachment directory = Attachment.builder()
            .driverId(driverId)
            .type(AttachmentType.Driver_Directory)
            .fsPath(tempDir.toString())
            .build();

        StepVerifier.create(fetcher.calculateSha1(directory))
            .expectNext(directory)
            .verifyComplete();
        assertThat(directory.getSha1()).isNull();
    }

    @Test
    void readsWholeFileAndRequestedRange(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path file = Files.writeString(tempDir.resolve("episode.mkv"), "0123456789");
        LocalDiskAttachmentDriverFetcher fetcher = createFetcher(driverId, tempDir);
        Attachment attachment = Attachment.builder()
            .driverId(driverId)
            .type(AttachmentType.Driver_File)
            .fsPath(file.toString())
            .path("/episode.mkv")
            .build();

        StepVerifier.create(readText(fetcher.getSteam(attachment)))
            .expectNext("0123456789")
            .verifyComplete();
        StepVerifier.create(readText(fetcher.getSteam(attachment, 2, 6)))
            .expectNext("23456")
            .verifyComplete();
        StepVerifier.create(fetcher.parseReadUrl(attachment))
            .expectNext(DRIVER_STATIC_RESOURCE_PREFIX + "/episode.mkv")
            .verifyComplete();
        StepVerifier.create(fetcher.parseDownloadUrl(attachment))
            .expectNext(DRIVER_STATIC_RESOURCE_PREFIX + "/episode.mkv")
            .verifyComplete();
    }

    private LocalDiskAttachmentDriverFetcher createFetcher(UUID driverId, Path rootPath) {
        LocalAttachmentPathValidator pathValidator = new LocalAttachmentPathValidator();
        pathValidator.register(driverId, rootPath.toString());
        return new LocalDiskAttachmentDriverFetcher(pathValidator);
    }

    private reactor.core.publisher.Mono<String> readText(
        reactor.core.publisher.Flux<DataBuffer> buffers) {
        return DataBufferUtils.join(buffers)
            .map(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            });
    }
}
