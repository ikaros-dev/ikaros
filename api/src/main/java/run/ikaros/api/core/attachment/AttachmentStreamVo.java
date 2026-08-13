package run.ikaros.api.core.attachment;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

@Data
public class AttachmentStreamVo {
    private @Nullable Flux<DataBuffer> dataBufferFlux;
    private @Nullable Long contextLength;
    private @Nullable String contextType;
}
