package run.ikaros.api.core.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 本地目录扫描出的单个附件预览信息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LocalScanItem {
    /** 附件标识。 */
    @JsonProperty("attachment_id")
    private UUID attachmentId;
    /** 相对绑定目录的路径。 */
    @JsonProperty("relative_path")
    private String relativePath;
    /** 文件的物理媒体类型。 */
    @JsonProperty("physical_type")
    private MediaPhysicalType physicalType;
    /** 文件在本次扫描中的业务角色。 */
    private MediaRole role;
    /** 用于展示文件语言、用途和声道等信息的元数据。 */
    @JsonProperty("display_metadata")
    private Map<String, String> displayMetadata;
    /** 自动关联或待确认关联的候选主资源附件标识。 */
    @JsonProperty("candidate_primary_attachment_id")
    private UUID candidatePrimaryAttachmentId;
    /** 媒体容器中探测到的轨道列表。 */
    private List<MediaTrack> tracks;
    /** 当前文件媒体轨道探测失败的原因。 */
    @JsonProperty("probe_failure_reason")
    private String probeFailureReason;
}
