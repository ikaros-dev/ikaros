package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 请求对本地目录进行无副作用扫描预览。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LocalScanPreviewRequest {
    /** 待扫描目录的附件标识。 */
    @JsonProperty("directory_id")
    private UUID directoryId;
    /** 扫描媒体时采用的模式。 */
    private LocalMediaMode mode;
}
