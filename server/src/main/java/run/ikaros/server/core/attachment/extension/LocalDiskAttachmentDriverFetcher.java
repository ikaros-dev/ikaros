package run.ikaros.server.core.attachment.extension;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
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
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;

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
    /** 本地文件名称门禁和真实格式检测服务。 */
    private final AttachmentMediaValidationService mediaValidationService;

    public LocalDiskAttachmentDriverFetcher(
        LocalAttachmentPathValidator pathValidator,
        AttachmentMediaValidationService mediaValidationService) {
        this.pathValidator = pathValidator;
        this.mediaValidationService = mediaValidationService;
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
                    .flatMap(file -> file.isDirectory()
                        ? createAttachment(driverId, parentAttId, file.toPath(),
                            AttachmentType.Driver_Directory)
                        : inspectFile(driverId, parentAttId, file.toPath()), 8);
            });
    }

    private Mono<Attachment> inspectFile(UUID driverId, UUID parentAttId, Path filePath) {
        String filename = filePath.getFileName().toString();
        if (!MediaFilePolicy.isAllowedFileName(filename)) {
            log.debug("Skip local driver file with unsupported name: {}", filename);
            return Mono.empty();
        }
        return Mono.fromCallable(() -> pathValidator.validateNow(driverId, filePath.toString()))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(realPath -> mediaValidationService.validate(realPath, filename)
                .then(createAttachment(driverId, parentAttId, realPath,
                    AttachmentType.Driver_File)))
            .onErrorResume(exception -> {
                log.debug("Skip invalid local driver media file: {}, reason={}",
                    filename, exception.getClass().getSimpleName());
                return Mono.empty();
            });
    }

    private Mono<Attachment> createAttachment(UUID driverId, UUID parentAttId, Path path,
                                               AttachmentType type) {
        return Mono.fromCallable(() -> {
                Path realPath = pathValidator.validateNow(driverId, path.toString());
                BasicFileAttributes attributes = Files.readAttributes(
                    realPath, BasicFileAttributes.class);
                return Attachment.builder()
                    .parentId(parentAttId)
                    .type(type)
                    .name(realPath.getFileName().toString())
                    .path(realPath.toString())
                    .url(realPath.toString())
                    .fsPath(realPath.toString())
                    .size(attributes.size())
                    .sha1("")
                    .updateTime(LocalDateTime.now())
                    .modifiedTime(LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(),
                        java.time.ZoneId.systemDefault()))
                    .deleted(false)
                    .driverId(driverId)
                    .build();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(IOException.class, exception -> {
                log.warn("Read local driver entry attributes failed: {}",
                    path.getFileName(), exception);
                return Mono.empty();
            });
    }

    @Override
    public Mono<String> parseReadUrl(Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null.");
        Assert.notNull(attachment.getId(), "Attachment id must not be null.");
        return Mono.just(OpenApiConst.ATT_STREAM_ENDPOINT_PREFIX + '/' + attachment.getId());
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
        buffer.limit((int) bytesToRead);
        buffer.limit((int) bytesToRead);

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
