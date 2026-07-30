package run.ikaros.server.core.attachment.extension;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;

/**
 * 读取本地磁盘目录及文件内容的附件驱动.
 */
@Slf4j
@Extension
@Component
public class LocalDiskAttachmentDriverFetcher implements AttachmentDriverFetcher {
    public static String LOCAL_DISK_DRIVER_NAME = "DISK";
    /** 本地驱动文件访问路径校验器. */
    private final LocalAttachmentPathValidator pathValidator;

    public LocalDiskAttachmentDriverFetcher(LocalAttachmentPathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

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
        return pathValidator.validate(driverId, remotePath)
            .flatMapMany(path -> {
                var files = path.toFile().listFiles();
                if (files == null) {
                    return Flux.error(
                        new IllegalArgumentException("目标路径不是可读取目录: " + remotePath));
                }
                return Flux.fromArray(files)
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .map(file -> {
                        long size = 0;
                        LocalDateTime modifiedTime = null;
                        try {
                            Path realFilePath = pathValidator.validateNow(
                                driverId, file.getAbsolutePath());
                            size = Files.size(realFilePath);
                            modifiedTime = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(realFilePath).toInstant(),
                                    ZoneId.systemDefault())
                                .truncatedTo(ChronoUnit.MICROS);
                        } catch (IOException ioException) {
                            log.warn("File metadata error: {}", ioException.getMessage());
                        }
                        return Attachment.builder()
                            .parentId(parentAttId)
                            .type(file.isFile()
                                ? AttachmentType.Driver_File
                                : AttachmentType.Driver_Directory)
                            .name(file.getName())
                            .path(file.getPath())
                            .url(file.getPath())
                            .fsPath(file.getAbsolutePath())
                            .size(size)
                            .modifiedTime(modifiedTime)
                            .updateTime(LocalDateTime.now())
                            .deleted(false)
                            .driverId(driverId)
                            .build();
                    })
                    .sequential();
            });
    }

    @Override
    public Mono<Attachment> calculateSha1(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        Assert.notNull(attachment.getDriverId(), "Attachment driverId must not be null.");
        Assert.hasText(attachment.getFsPath(), "Attachment fsPath must not be empty.");
        if (attachment.getType() != AttachmentType.Driver_File) {
            return Mono.just(attachment);
        }
        return pathValidator.validate(attachment.getDriverId(), attachment.getFsPath())
            .publishOn(Schedulers.boundedElastic())
            .map(path -> {
                try {
                    return attachment.setSha1(FileUtils.calculateSha1(path.toString()));
                } catch (IOException | NoSuchAlgorithmException exception) {
                    log.warn("File sha1 error: {}", exception.getMessage());
                    return attachment.setSha1("");
                }
            });
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
        return pathValidator.validate(att.getDriverId(), att.getFsPath())
            .flatMapMany(path -> org.springframework.core.io.buffer.DataBufferUtils
                .readAsynchronousFileChannel(
                    () -> AsynchronousFileChannel.open(path, StandardOpenOption.READ),
                    new DefaultDataBufferFactory(),
                    BUFFER_SIZE
                ));
    }

    @Override
    public Flux<DataBuffer> getSteam(Attachment att, long start, long end) {
        return pathValidator.validate(att.getDriverId(), att.getFsPath())
            .flatMapMany(path -> Flux.create(sink -> {
                try {
                    AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                        path, StandardOpenOption.READ);

                    AtomicLong position = new AtomicLong(start);
                    ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

                    readChunk(channel, buffer, position.get(), end, sink, () -> {
                        try {
                            channel.close();
                        } catch (IOException exception) {
                            sink.error(exception);
                        }
                    });

                } catch (IOException exception) {
                    sink.error(exception);
                }
            }));
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
