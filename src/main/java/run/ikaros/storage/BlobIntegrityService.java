package run.ikaros.storage;

import java.io.InputStream;
import java.util.UUID;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

/** Blob 校验公开边界；不允许业务层直接修改 Blob 内容身份。 */
public interface BlobIntegrityService {
    Mono<BlobIntegrityResult> verify(UUID blobId, String expectedSha256, long expectedSize,
                                      InputStream content);
    Mono<BlobIntegrityResult> verify(UUID blobId, String expectedSha256, long expectedSize,
                                      Flux<DataBuffer> content);
}
