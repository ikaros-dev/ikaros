package run.ikaros.api.core.attachment;

import lombok.Data;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

@Data
public class AttachmentStreamVo {
    private Flux<DataBuffer> dataBufferFlux;
    private Long contextLength;
    private String contextType;
}
