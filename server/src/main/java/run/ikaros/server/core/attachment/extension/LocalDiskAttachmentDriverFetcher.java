package run.ikaros.server.core.attachment.extension;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.infra.utils.FileUtils;
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
    public Flux<Attachment> getChildren(UUID driverId, UUID parentAttId, String remotePath) {
        Assert.hasText(remotePath, "remotePath must not be empty.");
        File file = new File(remotePath);
        Path path = Paths.get(remotePath);
        File[] files = path.toFile().listFiles();
        if (files == null) {
            return Flux.empty();
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
                .parentId(parentAttId)
                .type(f.isFile() ? AttachmentType.Driver_File : AttachmentType.Driver_Directory)
                .name(f.getName())
                .path(f.getPath())
                .url(f.getPath())
                .fsPath(f.getAbsolutePath())
                .size(size)
                .sha1(sha1)
                .updateTime(LocalDateTime.now())
                .deleted(false)
                .driverId(driverId)
                .build();
            attachments.add(attachment);
        }

        return Flux.fromStream(attachments.stream());
    }

    @Override
    public Mono<String> parseReadUrl(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        return Mono.just(DRIVER_STATIC_RESOURCE_PREFIX + attachment.getPath());
    }

    @Override
    public Mono<String> parseDownloadUrl(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        return parseReadUrl(attachment);
    }

    @Override
    public Flux<DataBuffer> getSteam(Attachment att) {
        File file = new File(att.getFsPath());
        Path path = Path.of(file.toURI());
        return org.springframework.core.io.buffer.DataBufferUtils
            .readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(path,
                    StandardOpenOption.READ),
                new DefaultDataBufferFactory(),
                BUFFER_SIZE
            );
    }

    @Override
    public Flux<DataBuffer> getSteam(Attachment att, long start, long end) {
        File file = new File(att.getFsPath());
        Path path = Path.of(file.toURI());
        return Flux.create(sink -> {
            try {
                AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    path, StandardOpenOption.READ);

                AtomicLong position = new AtomicLong(start);
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

                readChunk(channel, buffer, position.get(), end, sink, () -> {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        sink.error(e);
                    }
                });

            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    private void readChunk(AsynchronousFileChannel channel,
                           ByteBuffer buffer,
                           long position,
                           long end,
                           FluxSink<DataBuffer> sink,
                           Runnable onComplete) {

        if (position > end) {
            sink.complete();
            onComplete.run();
            return;
        }

        long bytesToRead = Math.min(buffer.capacity(), end - position + 1);

        channel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    sink.complete();
                    onComplete.run();
                    return;
                }

                attachment.flip();
                byte[] data = new byte[attachment.remaining()];
                attachment.get(data);

                DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(data);
                sink.next(dataBuffer);

                // 准备读取下一块
                attachment.clear();
                readChunk(channel, attachment, position + result, end, sink, onComplete);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                sink.error(exc);
                onComplete.run();
            }
        });
    }

}
