package run.ikaros.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;

/**
 * 受控本地文件 Provider 读取器。
 * Provider metadata 必须提供 rootPath，objectKey 只能解析到该根目录以内。
 */
@Service
public class LocalStorageContentReader implements StorageContentReader {
    private static final long MAX_RANGE_LENGTH = 64L * 1024L * 1024L;

    @Override
    public Mono<StorageContent> read(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob,
                                     String range) {
        if (!"LOCAL_FILESYSTEM".equalsIgnoreCase(provider.providerType())) {
            return Mono.error(new ConflictException("Storage Provider 不支持服务器端内容读取"));
        }
        Map<String, Object> metadata = provider.metadata();
        Object rootValue = metadata.get("rootPath");
        if (!(rootValue instanceof String rootText) || rootText.isBlank()) {
            return Mono.error(new ConflictException("本地 Storage Provider 缺少 rootPath 配置"));
        }
        Path root = Path.of(rootText).toAbsolutePath().normalize();
        Path file = root.resolve(placement.objectKey()).normalize();
        if (!file.startsWith(root)) {
            return Mono.error(new ConflictException("附件对象路径超出 Provider 根目录"));
        }
        return Mono.fromCallable(() -> {
            if (!Files.isRegularFile(file)) {
                throw new ConflictException("附件内容当前不可用");
            }
            long total = Files.size(file);
            long start = 0;
            long end = Math.max(0, total - 1);
            boolean partial = false;
            if (range != null && !range.isBlank()) {
                long[] bounds = parseRange(range, total);
                start = bounds[0];
                end = bounds[1];
                partial = true;
            }
            long length = total == 0 ? 0 : end - start + 1;
            long readStart = start;
            long readLength = length;
            Flux<DataBuffer> body = Flux.<DataBuffer>create(sink -> {
                try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                    channel.position(readStart);
                    long remaining = readLength;
                    while (remaining > 0) {
                        ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(64 * 1024, remaining));
                        int read = channel.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        buffer.flip();
                        sink.next(DefaultDataBufferFactory.sharedInstance.wrap(buffer));
                        remaining -= read;
                    }
                    sink.complete();
                } catch (Exception error) {
                    sink.error(error);
                }
            }).subscribeOn(Schedulers.boundedElastic());
            return new StorageContent(body, blob.mediaType(), length, total, start, end, partial);
        });
    }

    private long[] parseRange(String header, long total) {
        if (total == 0 || !header.startsWith("bytes=") || header.indexOf(',') >= 0) {
            throw new ConflictException("Range 请求无效");
        }
        String value = header.substring("bytes=".length()).trim();
        String[] parts = value.split("-", -1);
        if (parts.length != 2) {
            throw new ConflictException("Range 请求无效");
        }
        try {
            long start;
            long end;
            if (parts[0].isBlank()) {
                long suffix = Long.parseLong(parts[1]);
                if (suffix <= 0) throw new ConflictException("Range 请求无效");
                start = Math.max(0, total - suffix);
                end = total - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts[1].isBlank() ? total - 1 : Long.parseLong(parts[1]);
                if (start < 0 || start >= total || end < start) throw new ConflictException("Range 请求无效");
                end = Math.min(end, total - 1);
            }
            if (end - start + 1 > MAX_RANGE_LENGTH) throw new ConflictException("Range 请求超过单次读取上限");
            return new long[] {start, end};
        } catch (NumberFormatException error) {
            throw new ConflictException("Range 请求无效");
        }
    }

}
