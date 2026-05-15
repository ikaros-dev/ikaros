package run.ikaros.api.core.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

@Data
@Builder
public class AttachmentUploadCondition {

    @Schema(nullable = true, description = "父附件的ID，可为空，为空则代表附件在逻辑根目录下。")
    private UUID parentId;
    @Schema(description = "经过Basic64编码的附件名称，附件名称字段模糊查询。")
    private String name;
    @Schema(implementation = Flux.class, description = "附件的数据。")
    private Flux<DataBuffer> dataBufferFlux;
    @Schema(nullable = true, description = "当出现和已有文件相同名称，并且在同一父目录下时，是否重命名。")
    private Boolean isAutoReName;
}
