package run.ikaros.storage;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import run.ikaros.common.InvalidRangeException;
import run.ikaros.common.StorageUnavailableException;

/** S3-compatible 内容读取 adapter，支持单段 Range。 */
@Service
public class S3StorageContentReader implements StorageContentReader {
    private static final long MAX_RANGE_LENGTH = 64L * 1024L * 1024L;
    private final StorageCredentialResolver credentials;

    public S3StorageContentReader(StorageCredentialResolver credentials) { this.credentials = credentials; }

    @Override public boolean supports(StorageProvider provider) {
        String type = provider.providerType().toUpperCase();
        return type.equals("S3") || type.equals("AWS_S3") || type.equals("S3_COMPATIBLE")
            || type.equals("ALIYUN_OSS_S3") || type.equals("TENCENT_COS_S3");
    }

    @Override public Mono<StorageContent> read(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob, String range) {
        return credentials.resolve(provider.secretReference()).flatMap(creds -> Mono.fromCallable(() -> open(provider, placement, creds, range))
            .subscribeOn(Schedulers.boundedElastic()).map(handle -> content(handle, blob, range)));
    }

    private ReadHandle open(StorageProvider provider, BlobPlacementEntity placement, AwsCredentialsProvider creds, String range) {
        AbstractS3StorageObjectProvider.S3Settings settings = AbstractS3StorageObjectProvider.S3Settings.from(provider);
        S3Client client = S3Client.builder().region(Region.of(settings.region())).endpointOverride(settings.endpoint())
            .credentialsProvider(creds).build();
        try {
            GetObjectRequest.Builder request = GetObjectRequest.builder().bucket(settings.bucket()).key(placement.objectKey());
            if (range != null && !range.isBlank()) request.range(range);
            ResponseInputStream<?> stream = client.getObject(request.build());
            return new ReadHandle(client, stream);
        } catch (RuntimeException error) {
            client.close();
            throw new StorageUnavailableException("S3 附件内容读取失败: " + Optional.ofNullable(error.getMessage()).orElse("unknown"));
        }
    }

    private StorageContent content(ReadHandle handle, BlobEntity blob, String range) {
        long total = blob.sizeBytes(); long start = 0; long end = Math.max(0, total - 1); boolean partial = false;
        if (range != null && !range.isBlank()) { long[] bounds = parseRange(range, total); start = bounds[0]; end = bounds[1]; partial = true; }
        long length = total == 0 ? 0 : end - start + 1;
        Flux<DataBuffer> body = Flux.<DataBuffer>create(sink -> {
            try (handle) {
                byte[] buffer = new byte[64 * 1024]; int read;
                while ((read = handle.stream().read(buffer)) >= 0) { if (read > 0) sink.next(DefaultDataBufferFactory.sharedInstance.wrap(java.util.Arrays.copyOf(buffer, read))); }
                sink.complete();
            } catch (Exception error) { sink.error(error); }
        }).subscribeOn(Schedulers.boundedElastic());
        return new StorageContent(body, blob.mediaType(), length, total, start, end, partial);
    }

    private long[] parseRange(String header, long total) {
        if (total == 0 || !header.startsWith("bytes=") || header.indexOf(',') >= 0) throw new InvalidRangeException("Range 请求无效");
        String[] parts = header.substring(6).trim().split("-", -1); if (parts.length != 2) throw new InvalidRangeException("Range 请求无效");
        try { long start = parts[0].isBlank() ? Math.max(0, total - Long.parseLong(parts[1])) : Long.parseLong(parts[0]); long end = parts[1].isBlank() ? total - 1 : Long.parseLong(parts[1]); if (start < 0 || start >= total || end < start) throw new InvalidRangeException("Range 请求无效"); end = Math.min(end, total - 1); if (end - start + 1 > MAX_RANGE_LENGTH) throw new InvalidRangeException("Range 请求超过单次读取上限"); return new long[] {start, end}; }
        catch (NumberFormatException error) { throw new InvalidRangeException("Range 请求无效"); }
    }

    private record ReadHandle(S3Client client, InputStream stream) implements AutoCloseable {
        @Override public void close() { try { stream.close(); } catch (Exception ignored) { } client.close(); }
    }
}
