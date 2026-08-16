package run.ikaros.server.core.attachment.service;

import java.util.Objects;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.media.MediaFileDetectionResult;

/** 已完成真实格式检测并可按原顺序单次回放的媒体数据流. */
public record ValidatedMediaStream(
    // 真实格式检测结果。
    MediaFileDetectionResult detectionResult,
    // 按原始字节顺序单次回放的数据流。
    Flux<DataBuffer> content) {

    public ValidatedMediaStream {
        Objects.requireNonNull(detectionResult, "detectionResult must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
