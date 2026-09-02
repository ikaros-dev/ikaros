package run.ikaros.storage;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

/** 已解析的附件内容读取结果。 */
public record StorageContent(Flux<DataBuffer> body, String mediaType, long length, long totalLength,
                             long start, long end, boolean partial) {
}
