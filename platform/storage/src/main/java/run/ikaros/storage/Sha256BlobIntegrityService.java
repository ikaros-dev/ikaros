package run.ikaros.storage;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

/** 默认 SHA-256 完整性校验器，阻塞输入流由 boundedElastic 隔离。 */
@Service
public class Sha256BlobIntegrityService implements BlobIntegrityService {
    @Override
    public Mono<BlobIntegrityResult> verify(UUID blobId, String expectedSha256, long expectedSize,
                                            InputStream content) {
        if (blobId == null || expectedSha256 == null || expectedSize < 0 || content == null) {
            return Mono.error(new IllegalArgumentException("Blob 完整性校验参数不合法"));
        }
        return Mono.fromCallable(() -> {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            long size = 0;
            int read;
            while ((read = content.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
                size += read;
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            BlobIntegrityStatus status = size == expectedSize
                && actual.equalsIgnoreCase(expectedSha256)
                ? BlobIntegrityStatus.VERIFIED : BlobIntegrityStatus.CORRUPT;
            return new BlobIntegrityResult(status, actual, size);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .onErrorMap(NoSuchAlgorithmException.class, error -> new IllegalStateException("SHA-256 不可用", error));
    }

    @Override
    public Mono<BlobIntegrityResult> verify(UUID blobId, String expectedSha256, long expectedSize,
                                            Flux<DataBuffer> content) {
        if (blobId == null || expectedSha256 == null || expectedSize < 0 || content == null)
            return Mono.error(new IllegalArgumentException("Blob 完整性校验参数不合法"));
        return Mono.fromCallable(() -> MessageDigest.getInstance("SHA-256"))
            .flatMap(digest -> content.reduce(new long[] {0}, (state, buffer) -> {
                int readable = buffer.readableByteCount();
                byte[] bytes = new byte[readable];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                digest.update(bytes);
                state[0] += readable;
                return state;
            }).map(state -> {
                String actual = HexFormat.of().formatHex(digest.digest());
                BlobIntegrityStatus status = state[0] == expectedSize && actual.equalsIgnoreCase(expectedSha256)
                    ? BlobIntegrityStatus.VERIFIED : BlobIntegrityStatus.CORRUPT;
                return new BlobIntegrityResult(status, actual, state[0]);
            }))
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .onErrorMap(NoSuchAlgorithmException.class, error -> new IllegalStateException("SHA-256 不可用", error));
    }
}
