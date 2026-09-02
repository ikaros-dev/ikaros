package run.ikaros.storage;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
}
