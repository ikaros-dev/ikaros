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
            assertThat(attachment.getModifiedTime()).isNotNull();
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
    void calculateSha1ReturnsDirectoryWithoutHashing(@TempDir Path tempDir) {
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
    void calculateSha1RejectsFileChangedAfterScan(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path file = Files.writeString(tempDir.resolve("episode.mkv"), "episode-content");
        LocalDiskAttachmentDriverFetcher fetcher = createFetcher(driverId, tempDir);
        Attachment attachment = fetcher.getChildren(
                driverId, UUID.randomUUID(), tempDir.toString())
            .blockFirst();
        assertThat(attachment).isNotNull();
        attachment.setSize(attachment.getSize() + 1);

        StepVerifier.create(fetcher.calculateSha1(attachment))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("文件在扫描后发生变化")
                .hasMessageContaining(file.getFileName().toString()))
            .verify();
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
